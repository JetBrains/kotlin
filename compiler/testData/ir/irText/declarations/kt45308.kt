// FIR_IDENTICAL
// SKIP_KT_DUMP
// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6

// MODULE: a
// FILE: a.kt
package a

typealias A = String

// MODULE: b(a)
// FILE: b.kt
package b

import a.A

fun foo(f: () -> A): A = f()

// MODULE: c(b)
// FILE: c.kt
import b.foo

fun box(): String = foo { "OK" }
