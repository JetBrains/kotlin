// !CHECK_TYPE

import kotlin.reflect.KFunction0

class A {
    fun main() {
        val x = ::A

        checkSubtype<KFunction0<A>>(x)
    }
}

class SomeOtherClass {
    fun main() {
        val x = ::A

        checkSubtype<KFunction0<A>>(x)
    }
}
