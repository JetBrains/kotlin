// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57433

// FILE: test.kt

package test

fun callBuiltinFunctions(a: Int, b: Int) {
    a + b
    a or b
    a and b
    a.inv()
    a shl b
}
