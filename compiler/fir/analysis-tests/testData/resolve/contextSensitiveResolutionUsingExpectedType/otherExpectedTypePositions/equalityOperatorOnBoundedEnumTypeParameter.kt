// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76548
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

fun <X> id(x: X): X = TODO()

fun <T : MyEnum> foo(a: T) {
    val L = MyEnum.X

    if (a == X) {}
    if (a != X) {}
    if (a === X) {}
    if (a !== X) {}
    if (a == L) {}

    // We don't support context-sensitive resolution from LHS
    // Currently, type-checking/inference for different sides of equality operator is not reflective anyway
    if (<!UNRESOLVED_REFERENCE!>X<!> == a) {}
    if (L == a) {}

    // We only support context-sensitive resolution in equality for the simplest cases
    // Because otherwise, it seems to require changing how type-checking in general works for LHS/RHS
    if (a == id(<!UNRESOLVED_REFERENCE!>X<!>)) {}
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
        id(<!UNRESOLVED_REFERENCE!>X<!>) -> {}
        id(<!UNRESOLVED_REFERENCE!>Y<!>), id(L) -> {}
        else -> {}
    }

    when (val c = a) {
        id(<!UNRESOLVED_REFERENCE!>X<!>) -> {}
        id(<!UNRESOLVED_REFERENCE!>Y<!>), id(L) -> {}
        else -> {}
    }
}

/* GENERATED_FIR_TAGS: disjunctionExpression, enumDeclaration, enumEntry, equalityExpression, functionDeclaration,
ifExpression, localProperty, nullableType, propertyDeclaration, smartcast, typeConstraint, typeParameter, whenExpression,
whenWithSubject */
