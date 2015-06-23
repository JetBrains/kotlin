// !CHECK_TYPE
// FILE: a.kt

package a.b.c

class D<E, F> {
    fun foo(<!UNUSED_PARAMETER!>e<!>: E, <!UNUSED_PARAMETER!>f<!>: F) = this
}

// FILE: b.kt

import kotlin.reflect.KFunction3

fun main() {
    val x = a.b.c.D<String, Int>::foo

    checkSubtype<KFunction3<a.b.c.D<String, Int>, String, Int, a.b.c.D<String, Int>>>(x)
}
