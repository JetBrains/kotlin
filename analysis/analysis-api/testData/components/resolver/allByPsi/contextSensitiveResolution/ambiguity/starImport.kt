// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: foo.kt

package foo

object A
class B
class E
object F

object CompanionA
val CompanionB: Int = 0

// FILE: bar.kt

package bar

sealed class Sealed {
    data object A : Sealed()
    data class B(val x: Int) : Sealed()
    data object C : Sealed()
    data class D(val y: Int) : Sealed()
    data class E(val z: Int) : Sealed()
    data object F : Sealed()
    data class String(val t: kotlin.String) : Sealed()

    companion object {
        val CompanionA: Sealed = Sealed.A
        val CompanionB: Sealed = Sealed.A
    }
}

// FILE: main.kt

import foo.*
import foo.E
import foo.F
import bar.*

fun sealed(s: Sealed): Int = when (s) {
    A -> 1
    is B -> 2
    C -> 3
    is D -> 4
    is String -> 5
    CompanionA -> 6
    CompanionB -> 7
    is E -> 8
    is F -> 9
    else -> 100
}

fun sealedExplicit(s: Sealed): Int = when (s) {
    bar.Sealed.A -> 1
    is bar.Sealed.B -> 2
    is bar.Sealed.String -> 5
    bar.Sealed.CompanionA -> 6
    bar.Sealed.CompanionB -> 7
    is bar.Sealed.E -> 8
    is bar.Sealed.F -> 9
    else -> 100
}

fun topLevelExplicit(s: Sealed): Int = when (s) {
    foo.A -> 1
    is foo.B -> 2
    is kotlin.String -> 5
    foo.CompanionA -> 6
    foo.CompanionB -> 7
    is foo.E -> 8
    is foo.F -> 9
    else -> 6
}

fun cast1wrong(s: Sealed): Int {
    s as A
    return 1
}

fun cast2wrong(s: Sealed): Int {
    s as B
    return 2
}

fun cast1sealed(s: Sealed): Int {
    s as Sealed.A
    return 1
}

fun cast2sealed(s: Sealed): Int {
    s as Sealed.B
    return 2
}

fun cast1topLevel(s: Sealed): Int {
    s as foo.A
    return 1
}

fun cast2topLevel(s: Sealed): Int {
    s as foo.B
    return 2
}

fun cast3ok(s: Sealed): Int {
    s as C
    return 3
}

fun cast4ok(s: Sealed): Int {
    s as D
    return 4
}

fun equality1wrong(s: Sealed): Boolean = s == A
fun equality1sealed(s: Sealed): Boolean = s == Sealed.A
fun equality1topLevel(s: Sealed): Boolean = s == foo.A

fun equality2ok(s: Sealed): Boolean = s == Sealed.C

fun equality3wrong(s: Sealed): Boolean = s == CompanionA
fun equality3sealed(s: Sealed): Boolean = s == Sealed.CompanionA
fun equality3topLevel(s: Sealed): Boolean = s == foo.CompanionA

fun equality4ok(s: Sealed): Boolean = s == F
fun equality4sealed(s: Sealed): Boolean = s == Sealed.F
fun equality4topLevel(s: Sealed): Boolean = s == foo.F
