/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

object LocationTracker {
    fun initialize() {}

    val parameterAddedWhileThereAreAlreadyCalls = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()
    val parameterRemovedWhileThereAreAlreadyCalls = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()
    val argumentAddedForNonExistingParameter = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()
    val argumentAddedForParameterInsertedAfterCreation = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()

    fun recordStackTrace(registry: MutableSet<List<StackTraceElement>>, framesToSkip: Int = 0) {
        if (registry !== argumentAddedForNonExistingParameter) return
        throw IllegalStateException("!!!!!!!!!!!!!!")
    }
}