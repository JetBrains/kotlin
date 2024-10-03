/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

typealias LocationTracker_Registry = MutableMap<List<StackTraceElement>, MutableSet<List<StackTraceElement>>>

object LocationTracker {
    init {
        registerHook()
    }

    fun initialize() {}

    val attributeOwnerId: LocationTracker_Registry = hashMapOf()

    private fun registerHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            listOf(
                ::attributeOwnerId,
            ).forEach { prop ->
                val name = prop.name
                print("\n")
                println("!!! $name")
                prop.get().forEach { (readLocation, writeLocations) ->
                    print("READ: ")
                    printStackTrace(readLocation)
                    /*writeLocations.forEach { stack ->
                        print("WRITE: ")
                        printStackTrace(stack)
                    }*/
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
        var countAbove = 0
        val stack = Throwable().stackTrace
            .drop(framesToSkip + 1)
            .takeWhile { frame ->
                countAbove == 0 && (
                        frame.className.startsWith("org.jetbrains.kotlin.ir")
                                && !frame.className.startsWith("org.jetbrains.kotlin.ir.backend")
                        ) || countAbove++ < 1
            }
            .filter { it.className != "org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols" }
            .distinct()
        return stack
    }

    fun recordReadStackTrace(registry: LocationTracker_Registry, lastWriteStackTrace: List<StackTraceElement>?, framesToSkip: Int = 0) {
        var stack = Throwable().stackTrace.toList()
        if (stack[framesToSkip + 1].let {
                it.className == "org.jetbrains.kotlin.ir.declarations.IrDeclarationsKt"
                        && it.methodName.removeSuffix("\$default") == "copyAttributes"
            }
        ) return

        var countAbove = 0
        stack = stack
            .drop(framesToSkip + 1)
            .groupBy { it.className + it.methodName.removeSuffix("\$default") }.map { it.value[0] }
            .takeWhile { frame ->
                countAbove == 0 && (
                        frame.className.startsWith("org.jetbrains.kotlin.ir")
                                && !frame.className.startsWith("org.jetbrains.kotlin.ir.backend")
                        ) || countAbove++ < 1
            }
            .takeWhile {
                !it.className.startsWith("org.jetbrains.kotlin.ir.visitors.") &&
                        !(it.className.startsWith("org.jetbrains.kotlin.ir.") && (it.methodName == "accept" || it.methodName == "transform"))
            }
            .distinct()

        if (lastWriteStackTrace != null) {
            val set = registry.computeIfAbsent(stack) { hashSetOf() }
            set += lastWriteStackTrace
        }
    }
}