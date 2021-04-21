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
