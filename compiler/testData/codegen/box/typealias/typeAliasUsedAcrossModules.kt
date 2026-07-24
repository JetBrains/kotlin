// DUMP_IR
// MODULE: a
// FILE: a.kt
package a

typealias A = String

// MODULE: b(a)
// FILE: b.kt
package b

import a.A

fun foo(a: A, f: (A) -> A): A = f(a)

// MODULE: main(a, b)
// FILE: main.kt
import a.A
import b.foo

fun box(): String {
    val x: A = "OK"
    val res: A = foo(x) { it }
    return res
}
