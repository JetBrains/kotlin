/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.profiling

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

interface AsyncProfilerReflected {
    fun execute(command: String): String
    fun stop()
    val version: String
}



object AsyncProfilerHelper {
    private val profilerClass = Class.forName("one.profiler.AsyncProfiler")
    private val getInstanceHandle =
        MethodHandles.lookup().findStatic(profilerClass, "getInstance", MethodType.methodType(profilerClass, String::class.java))
    private val executeHandle =
        MethodHandles.lookup().findVirtual(
            profilerClass,
            "execute",
            MethodType.methodType(String::class.java, String::class.java)
        )
    private val stopHandle =
        MethodHandles.lookup().findVirtual(profilerClass, "stop", MethodType.methodType(Void.TYPE))
    private val getVersionHandle =
        MethodHandles.lookup().findVirtual(profilerClass, "getVersion", MethodType.methodType(String::class.java))

    fun getInstance(libPath: String?): AsyncProfilerReflected {
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

        }
    }
}