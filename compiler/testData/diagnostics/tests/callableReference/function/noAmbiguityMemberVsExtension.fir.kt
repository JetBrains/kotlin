// CHECK_TYPE

import kotlin.reflect.KFunction1

class A {
    fun foo() = 42
}

fun A.foo() {}

fun main() {
    val x = A::foo

    checkSubtype<KFunction1<A, Int>>(x)
}
