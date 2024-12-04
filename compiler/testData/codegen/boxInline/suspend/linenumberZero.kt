// WITH_STDLIB
// TARGET_BACKEND: JVM
// CHECK_BYTECODE_TEXT

// FILE: lib.kt
package a;

@Suppress("UNUSED_PARAMETER")
public fun a0(p: suspend () -> Unit) {
}

public inline fun a(crossinline p: suspend () -> Unit) = a0 { p() }

// FILE: main.kt

fun box(): String {
    a.a { }
    return "OK"
}

// 0 LINENUMBER 0 L*
