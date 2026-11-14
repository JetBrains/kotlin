/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.ExecutionContext
import org.jetbrains.kotlin.buildtools.internal.KotlinToolchainsImpl
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.classloading.LruClassLoadersCache
import org.jetbrains.kotlin.io.deleteOnExitRecursively
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class LruClassLoadersCacheTest {
    private val singleEntryCache = LruClassLoadersCache(1, javaClass.classLoader)
    private val tempDir = Files.createTempDirectory("classloaders-cache").also { it.deleteOnExitRecursively() }
    private val markerJar = createJar("marker")
    private val otherJar = createJar("other")

    @AfterEach
    fun tearDown() {
        singleEntryCache.close()
    }

    @Test
    fun `root cache closed prevents toOperationScoped`() {
        val rootCache = LruClassLoadersCache(10, javaClass.classLoader)
        rootCache.close()

        assertThrows(IllegalStateException::class.java) {
            rootCache.toOperationScoped(null)
        }
    }

    @Test
    fun `root cache closed prevents getForClassPath on scoped cache`() {
        val rootCache = LruClassLoadersCache(10, javaClass.classLoader)
        val scopedCache = rootCache.toOperationScoped(null)

        rootCache.close()

        val exception = assertThrows(IllegalStateException::class.java) {
            scopedCache.getForClassPath(listOf(File("test.jar")))
        }
        assertTrue(exception.message?.contains("was already closed") == true)
    }

    @Test
    fun `root cache closed prevents asPluginsLoader on scoped cache`() {
        val rootCache = LruClassLoadersCache(10, javaClass.classLoader)
        val scopedCache = rootCache.toOperationScoped(null)

        rootCache.close()

        val exception = assertThrows(IllegalStateException::class.java) {
            scopedCache.asPluginsLoader()
        }
        assertTrue(exception.message?.contains("was already closed") == true)
    }

    @Test
    fun `scoped cache closed prevents getForClassPath on that scoped cache only`() {
        val rootCache = LruClassLoadersCache(10, javaClass.classLoader)
        val scopedCache1 = rootCache.toOperationScoped(null)
        val scopedCache2 = rootCache.toOperationScoped(null)

        scopedCache1.close()

        assertThrows(IllegalStateException::class.java) {
            scopedCache1.getForClassPath(listOf(File("test.jar")))
        }

        // scopedCache2 and rootCache should still work
        assertDoesNotThrow {
            scopedCache2.getForClassPath(emptyList())
        }
        assertDoesNotThrow {
            rootCache.getForClassPath(emptyList())
        }

        rootCache.close()
    }

    @Test
    fun `cache size zero is rejected`() {
        assertThrows<IllegalArgumentException> {
            LruClassLoadersCache(0, javaClass.classLoader)
        }
    }

    @Test
    fun `cache retrieves and reuses classloader when size is positive`() {
        val rootCache = LruClassLoadersCache(10, javaClass.classLoader)
        val scopedCache1 = rootCache.toOperationScoped(null)

        val cl1 = scopedCache1.getForClassPath(emptyList())
        val cl2 = scopedCache1.getForClassPath(emptyList())
        assertSame(cl1, cl2)

        scopedCache1.close()

        val scopedCache2 = rootCache.toOperationScoped(null)
        val cl3 = scopedCache2.getForClassPath(emptyList())
        assertSame(cl1, cl3)

        scopedCache2.close()
        rootCache.close()
    }

    @Test
    fun `evicted classloader stays open while scope holds it and closes on scope close`() {
        val scope = singleEntryCache.toOperationScoped(null)
        val classLoader = scope.getForClassPath(listOf(markerJar))

        evictAll()
        assertOpen(classLoader)

        scope.close()
        assertClosed(classLoader)
    }

    @Test
    fun `evicted classloader closes immediately when no scope holds it`() {
        val classLoader = singleEntryCache.toOperationScoped(null).use { it.getForClassPath(listOf(markerJar)) }
        assertOpen(classLoader)

        evictAll()
        assertClosed(classLoader)
    }

    @Test
    fun `evicted classloader held by two scopes closes only after both scopes close`() {
        val scope1 = singleEntryCache.toOperationScoped(null)
        val scope2 = singleEntryCache.toOperationScoped(null)
        val classLoader = scope1.getForClassPath(listOf(markerJar))
        assertSame(classLoader, scope2.getForClassPath(listOf(markerJar)))

        evictAll()
        scope1.close()
        assertOpen(classLoader)

        scope2.close()
        assertClosed(classLoader)
    }

    @Test
    fun `build session closes operation scope when operation fails`() {
        val toolchains = KotlinToolchainsImpl()
        val operation = FailingOperation()

        toolchains.createBuildSession().use { session ->
            val exception = assertThrows<RuntimeException> {
                session.executeOperation(operation, toolchains.createInProcessExecutionPolicy(), null)
            }
            assertEquals("compilation failed", exception.message)
        }

        val scope = checkNotNull(operation.scope)
        val exception = assertThrows<IllegalStateException> {
            scope.getForClassPath(emptyList())
        }
        assertTrue(exception.message?.contains("was already closed") == true)
    }

    private class FailingOperation(override val options: Options = Options(FailingOperation::class)) : BuildOperationImpl<Unit>() {
        var scope: LruClassLoadersCache? = null

        override fun executeImpl(
            projectId: ProjectId,
            executionPolicy: ExecutionPolicy,
            logger: KotlinLogger?,
            executionContext: ExecutionContext,
        ) {
            scope = checkNotNull(executionContext.classloadersCache)
            scope!!.getForClassPath(emptyList())
            throw RuntimeException("compilation failed")
        }

        override val usesApplicationEnvironment: Boolean
            get() = false
    }

    private companion object {
        const val MARKER = "marker.txt"
    }

    private fun evictAll() {
        singleEntryCache.getForClassPath(listOf(otherJar))
    }

    private fun assertOpen(classLoader: URLClassLoader) = assertNotNull(classLoader.getResource(MARKER))

    private fun assertClosed(classLoader: URLClassLoader) = assertNull(classLoader.getResource(MARKER))

    private fun createJar(name: String): File {
        val jar = tempDir.resolve("$name.jar")
        JarOutputStream(Files.newOutputStream(jar)).use { out ->
            out.putNextEntry(JarEntry(MARKER))
            out.closeEntry()
        }
        return jar.toFile()
    }
}
