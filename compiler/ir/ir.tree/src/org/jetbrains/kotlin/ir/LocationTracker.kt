/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import java.util.stream.Collectors

typealias LocationTracker_Registry = MutableMap<StackTraceElement, MutableSet<List<StackTraceElement>>>

object LocationTracker {
    init {
        registerHook()
    }

    fun initialize() {}

    val attributeOwnerId: LocationTracker_Registry = hashMapOf()
    val originalBeforeInline: LocationTracker_Registry = hashMapOf()

    private fun registerHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            listOf(
                ::originalBeforeInline,
                ::attributeOwnerId,
            ).forEach { prop ->
                val name = prop.name
                print("\n")
                println("!!! $name")
                prop.get().forEach { (readLocation, writeLocations) ->
                    println("$readLocation")
                    writeLocations.forEach { stack ->
                        printStackTrace(stack)
                    }
                    print("\n")
                }
            }
        })
    }

    private fun printStackTrace(stack: List<StackTraceElement>) {
        stack.firstOrNull()?.let { println(it) }
        println(stack.drop(1).joinToString("\n") { "   $it" })
    }

    fun recordWriteStackTrace(framesToSkip: Int = 0): List<StackTraceElement> {
        val stack = StackWalker.getInstance().walk { stack ->
            var countAbove = 0
            stack
                .skip((framesToSkip + 1).toLong())
                .takeWhile { frame ->
                    countAbove == 0 && (
                            frame.className.startsWith("org.jetbrains.kotlin.ir")
                                    && !frame.className.startsWith("org.jetbrains.kotlin.ir.backend")
                            ) || countAbove++ < 1
                }
                .filter { it.className != "org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols" }
                .map { it.toStackTraceElement() }
                .distinct()
                .collect(Collectors.toList())
        }
        return stack
    }

    fun recordReadStackTrace(registry: LocationTracker_Registry, lastWriteStackTrace: List<StackTraceElement>?, framesToSkip: Int = 0) {
        if (lastWriteStackTrace == null)
            return

        val location = StackWalker.getInstance().walk { stack ->
            stack.skip((framesToSkip + 1).toLong())
                .findFirst().get()
        }

        if (location.className == "org.jetbrains.kotlin.ir.declarations.IrDeclarationsKt" && location.methodName == "copyAttributes")
            return
        if (location.className == "org.jetbrains.kotlin.ir.IrAttributeKt" && location.methodName == "unwrapAttributeOwner")
            return

        val set = registry.computeIfAbsent(location.toStackTraceElement()) { hashSetOf() }
        set += lastWriteStackTrace
    }
}