// FIR_IDENTICAL
// LANGUAGE: +ProhibitNonExhaustiveWhenOnAlgebraicTypes
// ISSUE: KT-48653

sealed class Sealed {
    object A : Sealed()
    object B : Sealed()
}
fun functionReturningSealed(): Sealed = null!!

fun test_1() {
    <!NO_ELSE_IN_WHEN!>when<!> (val result = functionReturningSealed()) {
        is Sealed.A -> {}
    }
}

fun test_2() {
    val result2 = functionReturningSealed()
    <!NO_ELSE_IN_WHEN!>when<!> (result2) {
        is Sealed.A -> {}
    }
}
