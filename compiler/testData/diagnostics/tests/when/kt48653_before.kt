// FIR_IDENTICAL
// LANGUAGE: -ProhibitNonExhaustiveWhenOnAlgebraicTypes
// ISSUE: KT-48653

sealed class Sealed {
    object A : Sealed()
    object B : Sealed()
}
fun functionReturningSealed(): Sealed = null!!

fun test_1() {
    <!NON_EXHAUSTIVE_WHEN_STATEMENT!>when<!> (val result = functionReturningSealed()) {
        is Sealed.A -> {}
    }
}

fun test_2() {
    val result2 = functionReturningSealed()
    <!NON_EXHAUSTIVE_WHEN_STATEMENT!>when<!> (result2) {
        is Sealed.A -> {}
    }
}
