// !CHECK_TYPE

import kotlin.reflect.*

fun main() {
    fun foo() {}
    fun bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
    fun baz() = "OK"
    
    class A {
        val x = ::foo
        val y = ::bar
        val z = ::baz

        fun main() {
            checkSubtype<KFunction0<Unit>>(x)
            checkSubtype<KFunction1<Int, Unit>>(y)
            checkSubtype<KFunction0<String>>(z)
        }
    }
}
