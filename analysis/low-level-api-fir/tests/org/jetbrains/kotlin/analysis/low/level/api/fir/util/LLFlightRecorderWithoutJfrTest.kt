/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame

/**
 * Checks that [LLFlightRecorder] works on a runtime without the `jdk.jfr` module (e.g., a custom `jlink`-ed JDK).
 */
class LLFlightRecorderWithoutJfrTest {
    @Test
    fun testRecorderIsNoOpWithoutJfr() {
        val classLoader = JfrHidingClassLoader(LLFlightRecorder::class.java.classLoader)
        assertFailsWith<ClassNotFoundException> { Class.forName("jdk.jfr.Event", false, classLoader) }

        val recorderClass = Class.forName(LLFlightRecorder::class.java.name, true, classLoader)
        assertNotSame(LLFlightRecorder::class.java, recorderClass)

        val recorder = recorderClass.getField("INSTANCE").get(null)
        recorderClass.getMethod(LLFlightRecorder::stopWorldSessionInvalidationScheduled.name).invoke(recorder)
        recorderClass.getMethod(LLFlightRecorder::stopWorldSessionInvalidationComplete.name).invoke(recorder)
    }

    /**
     * Hides `jdk.jfr` and loads classes from the [LLFlightRecorder] package by itself, so they resolve `jdk.jfr` through this loader.
     */
    private class JfrHidingClassLoader(parent: ClassLoader) : ClassLoader(parent) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            synchronized(getClassLoadingLock(name)) {
                if (name.startsWith("jdk.jfr.")) {
                    throw ClassNotFoundException(name)
                }

                if (!name.startsWith(ISOLATED_PACKAGE_PREFIX)) {
                    return super.loadClass(name, resolve)
                }

                findLoadedClass(name)?.let { return it }

                val bytes = parent.getResourceAsStream(name.replace('.', '/') + ".class")?.use { it.readBytes() }
                    ?: throw ClassNotFoundException(name)

                return defineClass(name, bytes, 0, bytes.size)
            }
        }

        companion object {
            private val ISOLATED_PACKAGE_PREFIX = LLFlightRecorder::class.java.name.substringBeforeLast('.') + "."
        }
    }
}
