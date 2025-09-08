// TARGET_BACKEND: JVM_IR, JS_IR, JS_IR_ES6, WASM
// FIR_IDENTICAL
// DUMP_IR
// WITH_STDLIB
// ISSUE: KT-62598
// This test is a simplified version of annotations/instances/multimoduleCreation.kt with potential deserialized annotation resolve problems

// JVM_ABI_K1_K2_DIFF: K2 serializes annotation parameter default values (KT-59526).

// MODULE: lib
// FILE: AnnotationInstantiationWithArrayLib.kt

package a

annotation class Outer(
    val array: Array<Inner> = [Inner([1]), Inner([2]), Inner([])]
) {
    annotation class Inner(val v: IntArray = [])
}

// MODULE: app(lib)
// FILE: AnnotationInstantiationWithArrayApp.kt

package test

import a.*

class C {
    fun six(): Outer = Outer()
}

fun box(): String {
    C().six().toString()
    return "OK"
}
