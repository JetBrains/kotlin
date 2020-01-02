// !CHECK_TYPE

import kotlin.reflect.*

class A

fun A.foo() {}
fun A.bar(x: Int) {}
fun A.baz() = "OK"

fun main() {
    val x = A::foo
    val y = A::bar
    val z = A::baz

    checkSubtype<KFunction1<A, Unit>>(x)
    checkSubtype<KFunction2<A, Int, Unit>>(y)
    checkSubtype<KFunction1<A, String>>(z)

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><KFunction<Unit>>(x)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><KFunction<Unit>>(y)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><KFunction<String>>(z)
}
