// !CHECK_TYPE

import kotlin.reflect.*

class A {
    fun foo() {}
    fun bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
    fun baz() = "OK"
}

class B {
    fun A.main() {
        val x = ::foo
        val y = ::bar
        val z = ::baz

        checkSubtype<KFunction1<A, Unit>>(x)
        checkSubtype<KFunction2<A, Int, Unit>>(y)
        checkSubtype<KFunction1<A, String>>(z)
    }
}
