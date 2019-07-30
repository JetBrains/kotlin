@file:Suppress("UNUSED_PARAMETER")

package sample

expect interface A

fun useA(block: A.() -> Unit) {}

fun anotherUseA(block: (A) -> Unit) {}