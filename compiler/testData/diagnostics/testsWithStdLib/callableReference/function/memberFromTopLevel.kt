// !CHECK_TYPE

import kotlin.reflect.*

class A {
    fun foo() {}
    fun bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
    fun baz() = "OK"
}

fun main() {
    val x = A::foo
    val y = A::bar
    val z = A::baz

    checkSubtype<KMemberFunction0<A, Unit>>(x)
    checkSubtype<KMemberFunction1<A, Int, Unit>>(y)
    checkSubtype<KMemberFunction0<A, String>>(z)
}
