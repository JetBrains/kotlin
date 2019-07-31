@file:Suppress("UNUSED_PARAMETER")

package sample

expect interface A {
    fun common_1_A()
}

expect interface B : A {
    fun common_1_B()
}

fun getB(): B = null!!

class Out<out T>(val value: T)

fun takeOutA_common_1(t: Out<A>) {}
fun takeOutB_common_1(t: Out<B>) {}