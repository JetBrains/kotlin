// !CHECK_TYPE

import kotlin.reflect.KFunction1

class A {
    fun foo() = 42
}

fun A.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() {}

fun main() {
    val x = A::foo

    checkSubtype<KFunction1<A, Int>>(x)
}
