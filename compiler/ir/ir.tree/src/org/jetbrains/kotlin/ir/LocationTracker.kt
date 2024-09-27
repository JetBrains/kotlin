/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

object LocationTracker {
    init {
        registerHook()
    }

    fun initialize() {}

    val parameterAddedWhileThereAreAlreadyCalls = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()
    val parameterRemovedWhileThereAreAlreadyCalls = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()
    val argumentAddedForNonExistingParameter = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()
    val argumentAddedForParameterInsertedAfterCreation = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()

    private fun registerHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            listOf(
                ::parameterAddedWhileThereAreAlreadyCalls,
                ::parameterRemovedWhileThereAreAlreadyCalls,
                ::argumentAddedForNonExistingParameter,
                ::argumentAddedForParameterInsertedAfterCreation
            ).forEach { prop ->
                val name = prop.name
                val set = prop.get()
                println()
                println("!!! $name")
                set.forEach { stack ->
                    stack.firstOrNull()?.let { println(it) }
                    println(stack.drop(1).joinToString("\n") { "  $it" })
                    println()
                }
                println()
            }
        })
    }

    fun recordStackTrace(registry: MutableSet<List<StackTraceElement>>, framesToSkip: Int = 0) {
        var stack = StackWalker.getInstance().walk { stack ->
            var countAbove = 0
            stack
                .skip((framesToSkip + 1).toLong())
                //.limit(20)
                .takeWhile { frame ->
                    countAbove == 0 && (
                            frame.className.startsWith("org.jetbrains.kotlin.ir")
                                    && !frame.className.startsWith("org.jetbrains.kotlin.ir.backend")
                            ) || countAbove++ < 1
                }
                .collect(Collectors.toList())
        }

        stack = stack.filterNot {
            it.className.removePrefix("org.jetbrains.kotlin.ir.visitors.").let { name ->
                name == "IrElementVisitor" || name == "IrElementVisitorVoid" ||
                        name == "IrVisitor" || name == "IrVisitorVoid" ||
                        name == "IrElementTransformer" || name == "IrElementTransformerVoid" ||
                        name == "IrTransformer" || name == "IrTransformerVoid"
            } || it.className == "org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols"
                    || it.className.substringAfterLast('.').startsWith("Ir") && (it.methodName == "accept" || it.methodName == "transform")
        }

        //println(stack.joinToString("\n") { "  $it"})
        registry += stack
            .map { it.toStackTraceElement() }
            .distinct()
    }
}