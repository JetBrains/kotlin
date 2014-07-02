// FILE: a.kt

package a.b.c

class D<E, F> {
    fun foo(<!UNUSED_PARAMETER!>e<!>: E, <!UNUSED_PARAMETER!>f<!>: F) = this
}

// FILE: b.kt

import kotlin.reflect.KMemberFunction2

fun main() {
    val x = a.b.c.D<String, Int>::foo

    x : KMemberFunction2<a.b.c.D<String, Int>, String, Int, a.b.c.D<String, Int>>
}
