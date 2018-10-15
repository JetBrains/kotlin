<!DIRECTIVES("HELPERS: REFLECT")!>

package org.jetbrains.<!ELEMENT(1)!>

open class <!ELEMENT(2)!>
open class <!ELEMENT(3)!><T>

typealias D<<!ELEMENT(6)!>> = <!ELEMENT(3)!><<!ELEMENT(6)!>>

inline fun <reified <!ELEMENT(4)!>, reified <!ELEMENT(5)!>> f1() =
    when (<!ELEMENT(2)!>()) {
        is <!ELEMENT(4)!> -> true
        is <!ELEMENT(5)!> -> false
        else -> false
    }

inline fun <reified T : D<<!ELEMENT(2)!>>> T.f2(value: T) = value is <!ELEMENT(2)!>

class A<K: List<out <!ELEMENT(3)!><out String>>> {
    val x = true
}

class B<K, T: A<List<out <!ELEMENT(3)!><String>>>> {
    var x = false
}

fun <T : org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(2)!>> T.f3() = false

fun f4(x1: List<out <!ELEMENT(3)!><String>>): Boolean {
    return true
}

fun f5(x1: List<List<List<<!ELEMENT(2)!>?>>>) = false

fun f6(x1: <!ELEMENT(2)!>) = false

fun f7(x1: <!ELEMENT(3)!><*>) = true

fun f8(x1: <!ELEMENT(3)!><out Any>) = false

fun f9(x1: <!ELEMENT(3)!><out List<<!ELEMENT(3)!><*>>>) = true

val x1: List<<!ELEMENT(2)!>?> = listOf(<!ELEMENT(2)!>(), null, <!ELEMENT(2)!>())

lateinit var x2: List<<!ELEMENT(3)!><out Number>?>

fun box(): String? {
    x2 = listOf(<!ELEMENT(3)!><Int>(), null, <!ELEMENT(3)!>())

    if (!f1<<!ELEMENT(2)!>, <!ELEMENT(3)!><<!ELEMENT(2)!>>>()) return null
    if (<!ELEMENT(3)!><<!ELEMENT(2)!>>().f2(<!ELEMENT(3)!>())) return null
    if (!A<List<<!ELEMENT(3)!><String>>>().x) return null
    if (B<<!ELEMENT(2)!>, A<List<<!ELEMENT(3)!><String>>>>().x) return null
    if (<!ELEMENT(2)!>().f3()) return null
    if (!f4(listOf(<!ELEMENT(3)!>()))) return null
    if (f5(listOf(listOf(listOf(null, <!ELEMENT(2)!>(), null, <!ELEMENT(2)!>()))))) return null
    if (f6(<!ELEMENT(2)!>())) return null
    if (!f7(<!ELEMENT(3)!><Nothing>())) return null
    if (f8(<!ELEMENT(3)!><<!ELEMENT(2)!>>())) return null
    if (!f9(<!ELEMENT(3)!><List<<!ELEMENT(3)!><<!ELEMENT(2)!>>>>())) return null

    if (x1.containsAll(listOf(<!ELEMENT(2)!>(), null, <!ELEMENT(2)!>()))) return null
    if (x2.containsAll(listOf(<!ELEMENT(3)!><Int>(), null, <!ELEMENT(3)!>()))) return null

    if (!checkCallableTypeParametersWithUpperBounds(
            <!ELEMENT(3)!><<!ELEMENT(2)!>>::f2,
            listOf(
                Pair("T", listOf("org.jetbrains.<!ELEMENT(1)!>.D<org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(2)!>> /* = org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(2)!>> */"))
            )
        )) return null

    if (!checkClassTypeParametersWithUpperBounds(
            A::class,
            listOf(
                Pair("K", listOf("kotlin.collections.List<out org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><out kotlin.String>>"))
            )
        )) return null

    if (!checkClassTypeParametersWithUpperBounds(
            B::class,
            listOf(
                Pair("T", listOf("org.jetbrains.<!ELEMENT(1)!>.A<kotlin.collections.List<out org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><kotlin.String>>>"))
            )
        )) return null

    if (!checkCallableTypeParametersWithUpperBounds(
            <!ELEMENT(2)!>::f3,
            listOf(
                Pair("T", listOf("org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(2)!>"))
            )
        )) return null

    if (!checkParameterType(::f4, "x1", "kotlin.collections.List<out org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><kotlin.String>>")) return null

    if (!checkParameterType(::f5, "x1", "kotlin.collections.List<kotlin.collections.List<kotlin.collections.List<org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(2)!>?>>>")) return null

    if (!checkPropertyType(::x1, "kotlin.collections.List<org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(2)!>?>")) return null

    if (!checkPropertyType(::x2, "kotlin.collections.List<org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><out kotlin.Number>?>")) return null

    if (!checkParameterType(::f6, "x1", "org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(2)!>")) return null

    if (!checkParameterType(::f7, "x1", "org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><*>")) return null

    if (!checkParameterType(::f8, "x1", "org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><out kotlin.Any>")) return null

    if (!checkParameterType(::f9, "x1", "org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><out kotlin.collections.List<org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><*>>>")) return null

    return "OK"
}
