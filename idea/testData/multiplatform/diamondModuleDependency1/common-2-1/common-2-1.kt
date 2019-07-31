@file:Suppress("UNUSED_PARAMETER")

package sample

actual interface A {
    actual fun foo()
    fun bar()
}

fun take_A_common_2_1(x: A) {
    x.foo()
    x.bar()
}
