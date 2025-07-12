/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// ISSUE: KT-69497
// WITH_STDLIB
// IGNORE_BACKEND: NATIVE, JS_IR, JS_IR_ES6, WASM
// ^^^ KT-69497
// IGNORE_BACKEND: JVM_IR
// ^^^ KT-75642

// IGNORE_BACKEND_MULTI_MODULE: JVM_IR, JVM_IR_SERIALIZE
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR_SERIALIZE
// ^^^ KT-79083 MainKt$box$$inlined$bar$1 cannot be cast to MainKt$box$$inlined$bar$2

// FILE: lib.kt

var counter = 0

inline fun <T> runTwice(f: () -> T) = f() to f()

// FILE: main.kt

inline fun bar(crossinline test: () -> Int): Int {
    val x = runTwice {
        object {
            init { counter++ }
            fun foo() = test()
        }
    }
    return x.first.foo() + x.second.foo()
}

fun box(): String {
    val result = bar { 5 }
    if (result != 5+5) return "result = $result"
    if (counter != 1) return "counter = $counter"
    return "OK"
}