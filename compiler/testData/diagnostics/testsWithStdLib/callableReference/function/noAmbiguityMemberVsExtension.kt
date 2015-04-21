// !CHECK_TYPE

import kotlin.reflect.KMemberFunction0

class A {
    fun foo() = 42
}

fun A.foo() {}

fun main() {
    val x = A::foo

    checkSubtype<KMemberFunction0<A, Int>>(x)
}
