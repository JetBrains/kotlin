// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

fun <X> id(x: X): X = TODO()

fun foo(a: MyEnum) {
    val L = MyEnum.X

    if (a == X) {}
    if (a != X) {}
    if (a === X) {}
    if (a !== X) {}
    if (a == L) {}

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
        a === X -> {}
        a !== Y -> {}
        a == L -> {}
    }

    when (a) {
        X -> {}
        Y, L -> {}
    }

    when (val c = a) {
        X -> {}
        Y, L -> {}
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