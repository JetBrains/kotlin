// LL_FIR_DIVERGENCE
// LL tests don't have jvmTargetProvider, so no class is known to be a value object at run time there.
// See isValueObjectAtRuntime
// ISSUE: KT-81100
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// LANGUAGE: +FullValueClasses
// JVM_TARGET: 17
// IGNORE_DEXING
// FILE: lib.kt
package lib

value class IdentityCompiled(val x: Int, val y: Int)

// MODULE: main(lib)
// LANGUAGE: +FullValueClasses
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// FILE: main.kt
import lib.IdentityCompiled

value class ValueCompiled(val x: Int, val y: Int)

fun test(identity: IdentityCompiled, value: ValueCompiled) {
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>identity<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>value<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaFunction, primaryConstructor, propertyDeclaration,
value */
