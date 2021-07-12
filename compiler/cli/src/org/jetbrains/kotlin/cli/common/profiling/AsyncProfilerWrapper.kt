/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.profiling

import java.io.File
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.net.URLClassLoader

interface AsyncProfilerReflected {
    fun execute(command: String): String
    fun stop()
    val version: String
}

object AsyncProfilerHelper {
    private var instance: AsyncProfilerReflected? = null

    fun getInstance(libPath: String?): AsyncProfilerReflected {
        // JVM doesn't support loading a native library multiple times even in different class loaders, so we don't attempt to load
        // async-profiler again after the first use, which allows to profile the same compiler process multiple times,
        // for example in the compiler daemon scenario.
        instance?.let { return it }

        val profilerClass = loadAsyncProfilerClass(libPath)
        val getInstanceHandle =
            MethodHandles.lookup().findStatic(profilerClass, "getInstance", MethodType.methodType(profilerClass, String::class.java))
        val executeHandle =
            MethodHandles.lookup().findVirtual(
                profilerClass,
                "execute",
                MethodType.methodType(String::class.java, String::class.java)
            )
        val stopHandle =
            MethodHandles.lookup().findVirtual(profilerClass, "stop", MethodType.methodType(Void.TYPE))
        val getVersionHandle =
            MethodHandles.lookup().findVirtual(profilerClass, "getVersion", MethodType.methodType(String::class.java))

        val instance = getInstanceHandle.invokeWithArguments(libPath)
        return object : AsyncProfilerReflected {
            private val boundExecute = executeHandle.bindTo(instance)
            private val boundStop = stopHandle.bindTo(instance)
            private val boundGetVersion = getVersionHandle.bindTo(instance)

            override fun execute(command: String): String {
                return boundExecute.invokeWithArguments(command) as String
            }

            override fun stop() {
                boundStop.invokeWithArguments()
            }

            override val version: String
                get() = boundGetVersion.invokeWithArguments() as String

        }.also { this.instance = it }
    }

    private fun loadAsyncProfilerClass(libPath: String?): Class<*> {
        val fqName = "one.profiler.AsyncProfiler"
        return try {
            Class.forName(fqName)
        } catch (e: ClassNotFoundException) {
            if (libPath == null) throw e
            else {
                val directory = File(libPath).parentFile
                check(directory.isDirectory) { directory }
                val apiJar = directory.resolve("async-profiler.jar")
                if (!apiJar.exists())
                    error("To use async-profiler, either add it to the compiler classpath, or put async-profiler.jar at this path: $apiJar")
                val classLoader = URLClassLoader(arrayOf(apiJar.toURI().toURL()), null)
                classLoader.loadClass(fqName)
            }
        }
    }
}
