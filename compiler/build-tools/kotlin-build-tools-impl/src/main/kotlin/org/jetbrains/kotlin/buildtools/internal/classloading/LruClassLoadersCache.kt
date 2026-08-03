/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.classloading

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
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
    private val cache: Cache<CacheKey, URLClassLoader>,
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
            .removalListener<CacheKey, URLClassLoader> { [key, cl] ->
                check(key != null && cl != null)
                logger?.info("Removing classloader from cache: ${key.entries.map { it.path }}")
                cl.close()
            }.build()
    )

    fun withLogger(logger: KotlinLogger?): LruClassLoadersCache = LruClassLoadersCache(parentClassLoader, logger, cache)

    override fun getForClassPath(files: List<File>): URLClassLoader = getForClassPath(files, parentClassLoader)

    private fun getForClassPath(files: List<File>, parent: ClassLoader): URLClassLoader {
        val key = makeKey(files)
        return cache.get(key) {
            makeClassLoader(key, parent)
        }
    }

    /**
     * Gets or creates [ClassLoader] from [bottom] + [top] files.
     * When creating new [ClassLoader] it tries to get [top] from cache first and then create new ClassLoader from [bottom] files,
     * providing [top] [ClassLoader] as parent.
     * Useful when you have internal and external artifacts and internal ones can be references from other internal artefacts only.
     * So you can safely cache [ClassLoader] from external artifacts and use it for internal ones.
     */
    fun getForSplitPaths(bottom: List<File>, top: List<File>): ClassLoader {
        return if (bottom.isEmpty() || top.isEmpty()) {
            getForClassPath(bottom + top)
        } else {
            val key = makeKey(bottom + top)
            cache.get(key) {
                val parent = getForClassPath(top)
                makeClassLoader(makeKey(bottom), parent)
            }
        }
    }

    override fun close() {
        cache.invalidateAll()
    }

    private fun makeClassLoader(key: CacheKey, parent: ClassLoader): URLClassLoader {
        val cp = key.entries.map { it.path }
        logger?.info("Creating new classloader for classpath: $cp")
        return URLClassLoader(cp.toTypedArray(), parent)
    }

    private fun makeKey(files: List<File>): CacheKey {
        //probably should walk dirs content for actual last modified
        val entries = files.map { f -> ClasspathEntry(f.toURI().toURL(), f.lastModified()) }
        return CacheKey(entries)
    }

    private data class ClasspathEntry(val path: URL, val modificationTimestamp: Long)

    private data class CacheKey(val entries: List<ClasspathEntry>)

    @OptIn(ExperimentalCompilerApi::class)
    fun asPluginsLoader(): PluginsLoader = object : PluginsLoader {
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
