// !CHECK_TYPE

import kotlin.reflect.KFunction0

fun main() {
    class A
    
    fun A.foo() {
        val x = ::A
        checkSubtype<KFunction0<A>>(x)
    }
    
    fun Int.foo() {
        val x = ::A
        checkSubtype<KFunction0<A>>(x)
    }
}
