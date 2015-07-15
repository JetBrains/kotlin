// !CHECK_TYPE
// FILE: a.kt

package first

class A {
    fun foo() {}
    fun bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
    fun baz() = "OK"
}

// FILE: b.kt

package other

import kotlin.reflect.*

import first.A

fun main() {
    val x = first.A::foo
    val y = first.A::bar
    val z = A::baz

    checkSubtype<KFunction1<A, Unit>>(x)
    checkSubtype<KFunction2<A, Int, Unit>>(y)
    checkSubtype<KFunction1<A, String>>(z)
}
