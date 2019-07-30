@file:Suppress("UNUSED_PARAMETER")
package sample

expect interface B : A {
    fun foo_B_1()
}

fun take1(x: B): BB = null!!
fun take2(x: B): BB = null!!
fun take3(x: B): BB = null!!

fun getB(): B = null!!