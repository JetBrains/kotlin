// RUN_PIPELINE_TILL: FRONTEND
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
    if (<!PROBLEMATIC_EQUALS!>a == Y<!>) {}
    if (a != X) {}
    if (<!PROBLEMATIC_EQUALS!>a != Y<!>) {}
    if (a === X) {}
    if (<!EQUALITY_NOT_APPLICABLE!>a === Y<!>) {}
    if (a !== X) {}
    if (<!EQUALITY_NOT_APPLICABLE!>a !== Y<!>) {}
    if (a == L) {}
    if (a is Derived) {}
    if (id(a) is Derived) {}

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
        <!PROBLEMATIC_EQUALS!>a != Y<!> -> {}
    }

    when {
        a === X -> {}
        a == L -> {}
        a is Derived -> {}
        <!EQUALITY_NOT_APPLICABLE!>a !== Y<!> -> {}
    }


    when (a) {
        X -> {}
        <!PROBLEMATIC_EQUALS!>Y<!>, L -> {}
        is Derived -> {}
    }

    when (val c = a) {
        X -> {}
        <!PROBLEMATIC_EQUALS!>Y<!>, L -> {}
        is Derived -> {}
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

/* GENERATED_FIR_TAGS: classDeclaration, data, disjunctionExpression, equalityExpression, functionDeclaration,
ifExpression, isExpression, localProperty, nestedClass, nullableType, objectDeclaration, propertyDeclaration, sealed,
smartcast, typeConstraint, typeParameter, whenExpression, whenWithSubject */
