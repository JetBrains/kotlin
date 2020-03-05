/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

enum class TargetBackend(
    val isIR: Boolean,
    private val compatibleWithTargetBackend: TargetBackend? = null
) {
    ANY(false),
    JVM(false),
    JVM_OLD(false, JVM),
    JVM_IR(true, JVM),
    JS(false),
    JS_IR(true, JS),
    WASM(true);

    val compatibleWith get() = compatibleWithTargetBackend ?: ANY
}
