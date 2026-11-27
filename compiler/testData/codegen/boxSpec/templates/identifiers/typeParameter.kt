<!DIRECTIVES("HELPERS: REFLECT")!>

package org.jetbrains.<!ELEMENT(1)!>

open class <!ELEMENT(2)!> {
    val x1 = false
}
open class <!ELEMENT(3)!><T> {
    val x1 = true
}

typealias A<<!ELEMENT(4)!>> = <!ELEMENT(3)!><<!ELEMENT(4)!>>

class B<<!ELEMENT(5)!>, <!ELEMENT(6)!>> {
    val x1: <!ELEMENT(5)!> = <!ELEMENT(3)!><<!ELEMENT(6)!>>() as <!ELEMENT(5)!>
    val x2: A<<!ELEMENT(6)!>> = <!ELEMENT(3)!><<!ELEMENT(5)!>>() as A<<!ELEMENT(6)!>>
    val x3: <!ELEMENT(6)!> = <!ELEMENT(2)!>() as <!ELEMENT(6)!>
}

fun <<!ELEMENT(7)!>: Number> f1(): Boolean {
    val x1: <!ELEMENT(7)!> = 10 as <!ELEMENT(7)!>
    return false
}

inline fun <reified <!ELEMENT(8)!>, reified <!ELEMENT(9)!> : Any> <!ELEMENT(8)!>.f2() = true

val <<!ELEMENT(10)!>> <!ELEMENT(10)!>.x1: <!ELEMENT(3)!><Int>
    get() = <!ELEMENT(3)!><Int>()

fun box(): String? {
    val b = B<<!ELEMENT(3)!><<!ELEMENT(2)!>>, <!ELEMENT(2)!>>()

    if (!null.x1.x1) return null
    if (!b.x1.x1 || !b.x2.x1 || b.x3.x1) return null
    if (f1<Byte>()) return null
    if (!0.f2<Int, <!ELEMENT(2)!>>()) return null
    if (!(-1).x1.x1) return null

    if (!checkCallableTypeParameter(Any::x1, "<!ELEMENT_VALIDATION(10)!>")) return null
    if (!checkClassTypeParameters(B::class, listOf("<!ELEMENT_VALIDATION(2)!>", "<!ELEMENT_VALIDATION(5)!>"))) return null
    if (!checkTypeProperties(B::class, listOf(
            Pair("x1", "<!ELEMENT(5)!>"),
            Pair("x2", "org.jetbrains.<!ELEMENT(1)!>.A<<!ELEMENT(6)!>> /* = org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><<!ELEMENT(6)!>> */"),
            Pair("x3", "<!ELEMENT(6)!>")
        ))) return null

    return "OK"
}