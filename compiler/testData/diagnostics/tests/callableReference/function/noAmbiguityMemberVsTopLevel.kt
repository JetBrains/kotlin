// !CHECK_TYPE

import kotlin.reflect.KFunction1

fun foo() {}

class A {
    fun foo() {}
    
    fun main() {
        val x = ::foo

        checkSubtype<KFunction1<A, Unit>>(x)
    }
}
