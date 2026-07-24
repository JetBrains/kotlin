// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects, +FullValueClasses
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect final value class A(val s: String)
expect abstract value class Abstract(x: Int)
expect sealed value class Sealed(x: Int)

// MODULE: jvm()()(common)
// FILE: jvm.kt
actual <!VALUE_CLASS_OPEN!>open<!> value class A<!ACTUAL_MISSING!>(val s: String)<!>
actual abstract value class Abstract<!ACTUAL_MISSING!>(x: Int)<!>
actual sealed value class Sealed<!ACTUAL_MISSING!>(x: Int)<!>

class B : A("")

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, propertyDeclaration, stringLiteral, value */
