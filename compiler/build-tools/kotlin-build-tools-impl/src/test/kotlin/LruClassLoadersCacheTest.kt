/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.internal.classloading.LruClassLoadersCache
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class LruClassLoadersCacheTest {

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
    fun `cache size zero works without hanging and closes classloader on scope close`() {
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
}
