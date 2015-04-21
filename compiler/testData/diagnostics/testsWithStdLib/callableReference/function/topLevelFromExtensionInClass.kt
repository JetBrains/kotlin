// !CHECK_TYPE

import kotlin.reflect.*

class A

fun foo() {}
fun bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
fun baz() = "OK"

class B {
    fun A.main() {
        val x = ::foo
        val y = ::bar
        val z = ::baz

        checkSubtype<KFunction0<Unit>>(x)
        checkSubtype<KFunction1<Int, Unit>>(y)
        checkSubtype<KFunction0<String>>(z)
    }
}
