// !CHECK_TYPE

import kotlin.reflect.KFunction0

class A

class B {
    fun A.ext() {
        val x = ::A
        val y = ::B

        checkSubtype<KFunction0<A>>(x)
        checkSubtype<KFunction0<B>>(y)
    }
    
    fun B.ext() {
        val x = ::A
        val y = ::B

        checkSubtype<KFunction0<A>>(x)
        checkSubtype<KFunction0<B>>(y)
    }
}
