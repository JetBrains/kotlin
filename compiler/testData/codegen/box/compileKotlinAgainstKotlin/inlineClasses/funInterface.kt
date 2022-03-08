// WITH_STDLIB
// KT-44622
// MODULE: lib
// FILE: A.kt

package x

inline class A(val value: String)

fun interface B {
    fun method(a: A): String
}

// MODULE: main(lib)
// FILE: B.kt

package y

import x.*

val b = B { it.value }

fun box(): String = b.method(A("OK"))
