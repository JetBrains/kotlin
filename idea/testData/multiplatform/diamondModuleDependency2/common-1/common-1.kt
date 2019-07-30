@file:Suppress("UNUSED_PARAMETER")
package sample

interface AA
interface BB
interface CC
interface DD

expect interface A {
    fun foo_A()
}

fun take0(x: A): AA = null!!
fun take1(x: A): AA = null!!
fun take2(x: A): AA = null!!
fun take3(x: A): AA = null!!
fun take4(x: A): AA = null!!

fun test(x: A) {
    take4(x)
}