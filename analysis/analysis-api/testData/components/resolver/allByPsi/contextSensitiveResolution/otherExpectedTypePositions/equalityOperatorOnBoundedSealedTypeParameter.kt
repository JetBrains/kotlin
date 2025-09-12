// ISSUE: KT-76548
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed class MySealed {
    data object X : MySealed() {}
    data object Y {}

    class Derived : MySealed()
}

fun <X> id(x: X): X = TODO()

fun <T : MySealed> foo(a: T) {
    val L = MySealed.X

    if (a == X) {}
    if (a == Y) {}
    if (a != X) {}
    if (a != Y) {}
    if (a === X) {}
    if (a === Y) {}
    if (a !== X) {}
    if (a !== Y) {}
    if (a == L) {}
    if (a is Derived) {}
    if (id(a) is Derived) {}

    // We don't support context-sensitive resolution from LHS
    // Currently, type-checking/inference for different sides of equality operator is not reflective anyway
    if (X == a) {}
    if (L == a) {}

    // We only support context-sensitive resolution in equality for the simplest cases
    // Because otherwise, it seems to require changing how type-checking in general works for LHS/RHS
    if (a == id(X)) {}
    if (a == id(L)) {}

    when {
        a == X -> {}
        a != Y -> {}
    }

    when {
        a === X -> {}
        a == L -> {}
        a is Derived -> {}
        a !== Y -> {}
    }
    when (a) {
        X -> {}
        Y, L -> {}
        is Derived -> {}
    }

    when (val c = a) {
        X -> {}
        Y, L -> {}
        is Derived -> {}
    }

    when (a) {
        id(X) -> {}
        id(Y), id(L) -> {}
        else -> {}
    }

    when (val c = a) {
        id(X) -> {}
        id(Y), id(L) -> {}
        else -> {}
    }
}

// IGNORE_STABILITY_K1: candidates