// !CHECK_TYPE

import kotlin.reflect.*

class A {
    fun main() {
        val x = ::foo
        val y = ::bar
        val z = ::baz

        checkSubtype<KFunction1<A, Unit>>(x)
        checkSubtype<KFunction2<A, Int, Unit>>(y)
        checkSubtype<KFunction1<A, String>>(z)
    }
}

fun A.foo() {}
fun A.bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
fun A.baz() = "OK"
