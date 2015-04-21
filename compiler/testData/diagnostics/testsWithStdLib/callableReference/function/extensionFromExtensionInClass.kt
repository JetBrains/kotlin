// !CHECK_TYPE

import kotlin.reflect.*

class B

class A {
    fun B.main() {
        val x = ::foo
        val y = ::bar
        val z = ::baz

        checkSubtype<KExtensionFunction0<A, Unit>>(x)
        checkSubtype<KExtensionFunction1<A, Int, Unit>>(y)
        checkSubtype<KExtensionFunction0<A, String>>(z)
    }
}

fun A.foo() {}
fun A.bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
fun A.baz() = "OK"
