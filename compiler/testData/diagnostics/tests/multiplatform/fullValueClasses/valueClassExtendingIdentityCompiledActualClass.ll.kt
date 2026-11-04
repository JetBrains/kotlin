// LL_FIR_DIVERGENCE
// LL tests check `m1-common` with its own session, also with platform checkers, so they see the expect class `Base` there instead of its
// actual class.
// LL_FIR_DIVERGENCE
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

value class Sub(val x: Int) : Base()

// MODULE: m2-jvm(lib)()(m1-common)
// LANGUAGE: +FullValueClasses
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// FILE: jvm.kt
actual abstract value class Base actual constructor() : <!VALUE_CLASS_EXTENDS_VALUE_CLASS_COMPILED_AS_IDENTITY_CLASS!>lib.IdentityCompiled<!>()

value class PlatformSub(val x: Int) : <!VALUE_CLASS_EXTENDS_VALUE_CLASS_COMPILED_AS_IDENTITY_CLASS!>Base<!>()

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, propertyDeclaration, value */
