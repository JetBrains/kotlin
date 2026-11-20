// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs
// LANGUAGE_FEATURE_TOGGLED: CallableReferencesWithBoundCompanionDontWinOverDefaultImports
// FIR_DUMP
// WITH_STDLIB

// FILE: f1.kt
package p1

fun test() {
    String?::hashCode
    String::hashCode

    Int?::toString
    Int::toString

    val _: (String?) -> Int = String?::hashCode
    val _: (String) -> Int = String?::hashCode
    val _: (String.Companion?) -> Int = String?::<!INITIALIZER_TYPE_MISMATCH!>hashCode<!>
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
    WithCustomCompanion?::toString
    WithCustomCompanion::apply
    WithCustomCompanion?::apply
}

// FILE: f5.kt
package p5

object B {
    fun apply() {}
}

fun test() {
    B::toString
    B::apply

    B?::toString
    B?::apply
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, functionDeclaration, functionalType,
localProperty, nullableType, objectDeclaration, propertyDeclaration, unnamedLocalVariable */
