// FIR_IDENTICAL
// DUMP_IR
// ISSUE: KT-70328
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt
interface MySet<out E> {
    fun contains(e: @UnsafeVariance E): Boolean
}

// MODULE: main(lib)
// FILE: main.kt
interface MyMutableSet<E> : MySet<E>

fun box() = "OK"