// FIR_IDENTICAL
// !CHECK_TYPE

import kotlin.reflect.KFunction0

fun main() {
    class A

    class B {
        fun Int.foo() {
            val x = ::A
            checkSubtype<KFunction0<A>>(x)
        }
        fun <!EXPOSED_RECEIVER_TYPE!>A<!>.foo() {
            val x = ::A
            checkSubtype<KFunction0<A>>(x)
        }
    }
}
