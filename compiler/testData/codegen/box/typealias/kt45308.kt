// KT-45308 Psi2ir: "AssertionError: TypeAliasDescriptor expected" caused by using typealias from one module as a type in another module without a transitive dependency
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

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
