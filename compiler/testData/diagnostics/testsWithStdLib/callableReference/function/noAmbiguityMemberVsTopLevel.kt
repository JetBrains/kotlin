// !CHECK_TYPE

import kotlin.reflect.KMemberFunction0

fun foo() {}

class A {
    fun foo() {}
    
    fun main() {
        val x = ::foo

        checkSubtype<KMemberFunction0<A, Unit>>(x)
    }
}
