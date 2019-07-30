@file:Suppress("UNUSED_PARAMETER")

package sample

actual interface A {
    actual fun foo()
    fun baz()
}

fun take_A_common_2_2(x: A) {
    x.foo()
    x.baz()
}