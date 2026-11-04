// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// LANGUAGE: +FullValueClasses
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// IGNORE_DEXING
// FILE: lib.kt
package lib

abstract value class IdentityCompiled

// MODULE: m1-common(lib)
// LANGUAGE: +FullValueClasses
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// FILE: common.kt
expect abstract value class Base()

value class Sub(val x: Int) : <!VALUE_CLASS_EXTENDS_VALUE_CLASS_COMPILED_AS_IDENTITY_CLASS!>Base<!>()

fun commonUse(base: Base, sub: Sub) {
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE{METADATA}!>base<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE{METADATA}!>sub<!>)
}

// MODULE: m2-jvm(lib)()(m1-common)
// LANGUAGE: +FullValueClasses
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// FILE: jvm.kt
actual abstract value class Base actual constructor() : <!VALUE_CLASS_EXTENDS_VALUE_CLASS_COMPILED_AS_IDENTITY_CLASS!>lib.IdentityCompiled<!>()

fun platformUse(base: Base, sub: Sub) {
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>base<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>sub<!>)
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, javaFunction, primaryConstructor,
propertyDeclaration, value */
