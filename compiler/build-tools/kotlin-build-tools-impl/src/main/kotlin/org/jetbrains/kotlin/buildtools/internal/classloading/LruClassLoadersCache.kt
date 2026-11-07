/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.classloading

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.internal.CloseableGuard
import org.jetbrains.kotlin.cli.jvm.plugins.PluginsLoader
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.components.ClassLoadersCache
import org.jetbrains.kotlin.util.ServiceLoaderLite
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.time.Duration

/**
 * LRU cache for [ClassLoader]s by class path.
 */
internal class LruClassLoadersCache private constructor(
    private val parentClassLoader: ClassLoader = ClassLoader.getSystemClassLoader(),
    private val logger: KotlinLogger? = null,
    private val cache: Cache<CacheKey, ClassLoaderHolder>,
    private val rootGuard: CloseableGuard? = null,
    private val closeableGuard: CloseableGuard = CloseableGuard(LruClassLoadersCache::class),
    private val isRootCache: Boolean,
) : ClassLoadersCache, AutoCloseable {
    constructor(
        size: Int,
        parentClassLoader: ClassLoader,
        ttl: Duration = Duration.ofHours(1),
        logger: KotlinLogger? = null,
    ) : this(
        parentClassLoader,
        logger,
        cache = CacheBuilder.newBuilder().maximumSize(size.toLong()).expireAfterAccess(ttl)
            .removalListener<CacheKey, ClassLoaderHolder> { [key, cl] ->
                check(key != null && cl != null)
                logger?.info("Removing classloader from cache: ${key.entries.map { it.file }}")
                cl.close() // TODO: KT-91193 this causes problems when the classloader is still in use
            }.build(),
        rootGuard = null,
        isRootCache = true,
    ) {
        require(size > 0) { "Cache size must be greater than 0" }
    }

    private val accessedClassLoaders = mutableSetOf<ClassLoaderHolder>()

    @Synchronized
    fun toOperationScoped(logger: KotlinLogger?): LruClassLoadersCache {
        requireNotClosed()
        return LruClassLoadersCache(
            parentClassLoader,
            logger,
            cache,
            rootGuard = rootGuard ?: closeableGuard,
            isRootCache = false,
        )
    }

    @Synchronized
    override fun getForClassPath(files: List<File>): URLClassLoader {
        requireNotClosed()
        return getForClassPathInternal(files, parentClassLoader)
    }

    @Suppress("NOTHING_TO_INLINE") // inline to make our AutoCloseablesTest pass
    private inline fun requireNotClosed() {
        rootGuard?.requireNotClosed()
        closeableGuard.requireNotClosed()
    }

    @Synchronized
    private fun getForClassPathInternal(files: List<File>, parent: ClassLoader): URLClassLoader {
        val key = makeKey(files)
        while (true) {
            val holder = cache.get(key) {
                ClassLoaderHolder(makeClassLoader(key, parent))
            }
            val cl = synchronized(holder) {
                if (accessedClassLoaders.contains(holder)) {
                    holder.classLoader
                } else {
                    val acquired = holder.tryAcquire()
                    if (acquired != null) {
                        accessedClassLoaders.add(holder)
                        acquired
                    } else {
                        null
                    }
                }
            }
            if (cl != null) {
                return cl
            }
        }
    }

//    /**
//     * Gets or creates [ClassLoader] from [bottom] + [top] files.
//     * When creating new [ClassLoader] it tries to get [top] from cache first and then create new ClassLoader from [bottom] files,
//     * providing [top] [ClassLoader] as parent.
//     * Useful when you have internal and external artifacts and internal ones can be references from other internal artefacts only.
//     * So you can safely cache [ClassLoader] from external artifacts and use it for internal ones.
//     */
//    fun getForSplitPaths(bottom: List<File>, top: List<File>): ClassLoader {
//        closeableGuard.requireNotClosed()
//        return if (bottom.isEmpty() || top.isEmpty()) {
//            getForClassPath(bottom + top)
//        } else {
//            val key = makeKey(bottom + top)
//            cache.get(key) {
//                val parent = getForClassPath(top)
//                makeClassLoader(makeKey(bottom), parent)
//            }
//        }
//    }

    @Synchronized
    override fun close() {
        closeableGuard.close {
            if (isRootCache) {
                cache.invalidateAll()
            }
            accessedClassLoaders.forEach { it.release() }
        }
    }

    private fun makeClassLoader(key: CacheKey, parent: ClassLoader): URLClassLoader {
        val urls = key.entries.map { it.file.toURI().toURL() }
        logger?.info("Creating new classloader for classpath: $urls")
        return URLClassLoader(urls.toTypedArray(), parent)
    }

    private fun makeKey(files: List<File>): CacheKey {
        //probably should walk dirs content for actual last modified
        val entries = files.map { f -> ClasspathEntry(f, f.lastModified()) }
        return CacheKey(entries)
    }

    private data class ClasspathEntry(val file: File, val modificationTimestamp: Long)

    private data class CacheKey(val entries: List<ClasspathEntry>)

    @OptIn(ExperimentalCompilerApi::class)
    fun asPluginsLoader(): PluginsLoader {
        requireNotClosed()
        return object : PluginsLoader {
            override fun loadCompilerPluginRegistrars(
                pluginClasspath: Collection<String>,
                parentDisposable: Disposable,
            ): List<CompilerPluginRegistrar> {
                return ServiceLoaderLite.loadImplementations(
                    CompilerPluginRegistrar::class.java, getForClassPath(pluginClasspath.map { File(it) })
                )
            }

            override fun loadCommandLineProcessors(
                pluginClasspath: Collection<String>,
                parentDisposable: Disposable,
            ): List<CommandLineProcessor> {
                return ServiceLoaderLite.loadImplementations(
                    CommandLineProcessor::class.java, getForClassPath(pluginClasspath.map { File(it) })
                )
            }
        }
    }

    private class ClassLoaderHolder(val classLoader: URLClassLoader) {
        private var referenceCount = 0
        private var closing = false
        private var closed = false

        @Synchronized
        fun tryAcquire(): URLClassLoader? {
            if (closed || closing) return null
            referenceCount++
            return classLoader
        }

        @Synchronized
        fun release() {
            referenceCount--
            check(referenceCount >= 0) { "Reference count cannot be negative" }
            if (closing) {
                maybeCloseClassLoader()
            }
        }

        @Synchronized
        fun close() {
            if (!closed) {
                closing = true
                maybeCloseClassLoader()
            }
        }

        private fun maybeCloseClassLoader() {
            if (referenceCount == 0) {
                classLoader.close()
                closed = true
            }
        }
    }
}
