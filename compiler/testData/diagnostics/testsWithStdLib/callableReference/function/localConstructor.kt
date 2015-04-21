// !CHECK_TYPE

import kotlin.reflect.KFunction0

fun main() {
    class A
    
    val x = ::A
    checkSubtype<KFunction0<A>>(x)
}
