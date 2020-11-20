@file:Suppress("UNUSED_PARAMETER")
package sample

actual interface A {
    actual fun foo_A()
    fun foo_A_3()
}

fun take0(x: A): DD = null!!
fun take1(x: A): DD = null!!
fun take2(x: A): DD = null!!
fun take4(x: A): DD = null!!

fun test(x: A) {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>take4<!>(x)
}