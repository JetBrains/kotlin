// This test checks that unresolved typealias in an abbreviated type does not crash the compiler or result in a compilation error.
// Apparently, there's some demand for this behavior, see KT-45308.

// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: reports RETURN_TYPE_MISMATCH: Return type mismatch: expected kotlin/String, actual a/A

// MODULE: a
// FILE: a.kt
package a

typealias A = String

// MODULE: b(a)
// FILE: b.kt
package b

import a.A

fun foo(f: () -> A): A = f()

// MODULE: main(b)
// FILE: c.kt
import b.foo

fun box(): String = foo { "OK" }
