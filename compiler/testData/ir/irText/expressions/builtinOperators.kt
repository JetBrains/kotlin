// FIR_IDENTICAL
// NO_SIGNATURE_DUMP
// ^KT-57433

// FILE: test.kt

package test

fun callBuiltinFunctions(a: Int, b: Int) {
    a + b
    a or b
    a and b
    a.inv()
    a shl b
}
