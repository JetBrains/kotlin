// FIR_IDENTICAL
// CHECK_TYPE

import kotlin.reflect.*

class A

fun main() {
    fun foo() {}
    fun bar(x: Int) {}
    fun baz() = "OK"

    fun A.ext() {
        val x = ::foo
        val y = ::bar
        val z = ::baz

        checkSubtype<KFunction0<Unit>>(x)
        checkSubtype<KFunction1<Int, Unit>>(y)
        checkSubtype<KFunction0<String>>(z)
    }
}
