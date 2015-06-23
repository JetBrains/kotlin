// !CHECK_TYPE
// FILE: a.kt

package a.b.c

class D {
    fun foo() = 42
}

// FILE: b.kt

import kotlin.reflect.KFunction1

fun main() {
    val x = a.b.c.D::foo

    checkSubtype<KFunction1<a.b.c.D, Int>>(x)
}
