/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

enum class TargetBackend(
    private val compatibleWithTargetBackend: TargetBackend? = null
) {
    ANY,
    JVM,
    JVM_IR(JVM),
    JVM_IR_SERIALIZE(JVM_IR),
    JS_IR,
    JS_IR_ES6(JS_IR),
    WASM,
    WASM_JS(WASM),
    WASM_WASI(WASM),
    ANDROID(JVM),
    NATIVE,
    ;

    val compatibleWith get() = compatibleWithTargetBackend ?: ANY

    fun isTransitivelyCompatibleWith(backend: TargetBackend): Boolean {
        if (this == backend) return true
        return compatibleWithTargetBackend?.isTransitivelyCompatibleWith(backend) ?: false
    }
}
