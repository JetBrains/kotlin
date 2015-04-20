// !CHECK_TYPE

import kotlin.reflect.*

class A

fun A.foo() {}
fun A.bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
fun A.baz() = "OK"

fun main() {
    val x = A::foo
    val y = A::bar
    val z = A::baz

    checkSubtype<KExtensionFunction0<A, Unit>>(x)
    checkSubtype<KExtensionFunction1<A, Int, Unit>>(y)
    checkSubtype<KExtensionFunction0<A, String>>(z)
}
