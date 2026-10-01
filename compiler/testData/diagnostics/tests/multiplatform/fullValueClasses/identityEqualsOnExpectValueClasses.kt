// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +FullValueClasses
// LANGUAGE: -StrictEquals
//  ^^^ KT-88389

// MODULE: m1-common
// FILE: common.kt
expect value class Full(val x: Int, val y: Int)
expect abstract value class Abstract()

fun common(a: Full, b: Full, any: Any, abstract: Abstract) {
    <!FORBIDDEN_IDENTITY_EQUALS!>a === b<!>
    <!FORBIDDEN_IDENTITY_EQUALS!>a === any<!>
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>abstract === any<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual value class Full actual constructor(val x: Int, val y: Int)
actual abstract value class Abstract actual constructor()

fun platform(a: Full, b: Full, any: Any, abstract: Abstract) {
    <!FORBIDDEN_IDENTITY_EQUALS!>a === b<!>
    <!FORBIDDEN_IDENTITY_EQUALS!>a === any<!>
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>abstract === any<!>
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, equalityExpression, expect, functionDeclaration, primaryConstructor,
propertyDeclaration, value */
