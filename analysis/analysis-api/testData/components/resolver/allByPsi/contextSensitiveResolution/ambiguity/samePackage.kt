// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

package foo

sealed class Sealed {
    data object A : Sealed()
    data class B(val x: Int) : Sealed()
    data object C : Sealed()
    data class D(val y: Int) : Sealed()
    data class String(val t: kotlin.String) : Sealed()
}

object A
class B

fun sealed(s: Sealed): Int = when (s) {
    A -> 1
    is B -> 2
    C -> 3
    is D -> 4
    is String -> 5
    else -> 6
}

fun sealedExplicit(s: Sealed): Int = when (s) {
    Sealed.A -> 1
    is Sealed.B -> 2
    else -> 6
}

fun topLevelExplicit(s: Sealed): Int = when (s) {
    foo.A -> 1
    is foo.B -> 2
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
