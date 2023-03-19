/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    JVM_MULTI_MODULE_IR_AGAINST_OLD(true, JVM_IR),
    JVM_MULTI_MODULE_OLD_AGAINST_IR(false, JVM),
    JVM_IR_SERIALIZE(true, JVM_IR),
    JS(false),
    JS_IR(true, JS),
    JS_IR_ES6(true, JS_IR),
    WASM(true),
    ANDROID(false, JVM),
    ANDROID_IR(true, JVM_IR),
    NATIVE(true),
    NATIVE_WITH_LEGACY_MM(true, NATIVE),
    JVM_WITH_OLD_EVALUATOR(false),
    JVM_IR_WITH_OLD_EVALUATOR(true),
    JVM_WITH_IR_EVALUATOR(false),
    JVM_IR_WITH_IR_EVALUATOR(true);

    val compatibleWith get() = compatibleWithTargetBackend ?: ANY
}
