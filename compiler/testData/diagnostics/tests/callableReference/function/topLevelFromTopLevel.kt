// !CHECK_TYPE

import kotlin.reflect.*

fun foo() {}
fun bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
fun baz() = "OK"

fun main() {
    val x = ::foo
    val y = ::bar
    val z = ::baz

    checkSubtype<KFunction0<Unit>>(x)
    checkSubtype<KFunction1<Int, Unit>>(y)
    checkSubtype<KFunction0<String>>(z)
}
