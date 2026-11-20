// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs
// LANGUAGE_FEATURE_TOGGLED: CallableReferencesWithBoundCompanionDontWinOverDefaultImports
// FIR_DUMP
// WITH_STDLIB

// FILE: f1.kt
package p1

fun test() {
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>String<!>?::hashCode
    String::hashCode

    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>Int<!>?::toString
    Int::toString

    val _: (String?) -> Int = String?::hashCode
    val _: (String) -> Int = String?::hashCode
    val _: (String.Companion?) -> Int = <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>String<!>?::<!INITIALIZER_TYPE_MISMATCH!>hashCode<!>
    val _: (String.Companion?) -> Int = String.Companion?::hashCode
    val _: (String.Companion) -> Int = String::<!INITIALIZER_TYPE_MISMATCH!>hashCode<!>
}

// FILE: f2.kt
package p2

import kotlin.hashCode

fun test() {
    String?::hashCode
    String::hashCode
}

// FILE: f3.kt
package p3

class WithCustomCompanion {
    companion object {
        fun apply() {}
    }
}

fun test() {
    WithCustomCompanion::toString
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>WithCustomCompanion<!>?::toString
    WithCustomCompanion::apply
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>WithCustomCompanion<!>?::apply
}

// FILE: f5.kt
package p5

object B {
    fun apply() {}
}

fun test() {
    B::toString
    B::apply

    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>B<!>?::toString
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>B<!>?::apply
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, functionDeclaration, functionalType,
localProperty, nullableType, objectDeclaration, propertyDeclaration, unnamedLocalVariable */
