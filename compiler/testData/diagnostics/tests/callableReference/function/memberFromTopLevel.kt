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

    checkSubtype<KFunction1<A, Unit>>(x)
    checkSubtype<KFunction2<A, Int, Unit>>(y)
    checkSubtype<KFunction1<A, String>>(z)

    checkSubtype<KFunction<Unit>>(x)
    checkSubtype<KFunction<Unit>>(y)
    checkSubtype<KFunction<String>>(z)
}
