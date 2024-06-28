// FIR_IDENTICAL
// WITH_STDLIB
// WITH_REFLECT
// ISSUE: KT-59066

import kotlin.reflect.KProperty

interface M< E>

public operator fun <X, Y : X> M<out X>.getValue(thisRef: Any?, property: KProperty<*>): Y =
    TODO()

public operator fun <Z> M<in Z>.setValue(thisRef: Any?, property: KProperty<*>, value: Z) {}

fun <U> m(u: U): M<U> = TODO()

var a by m("")

var b by mutableMapOf("" to 1)

fun main() {
    a.length
    a = "b"

    b = 23
    b.and(4)
}