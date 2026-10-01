// LL_FIR_DIVERGENCE
// LL tests don't have jvmTargetProvider, so no class is known to be a value object at run time there.
// See isValueObjectAtRuntime
// ISSUE: KT-81100
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
// RENDER_DIAGNOSTICS_FULL_TEXT
import java.lang.ref.WeakReference

value class Full(val x: Int, val y: Int)

fun captured(array: Array<out Full>) {
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>array[0]<!>)
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>array[0]<!>) {}
}

fun references(full: Full) {
    listOf(full).map(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>System::identityHashCode<!>)
    listOf(full).map(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>::WeakReference<!>)
    val reference: (Full) -> WeakReference<Full> = <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>::WeakReference<!>
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, flexibleType, functionDeclaration, functionalType, integerLiteral,
javaCallableReference, javaFunction, lambdaLiteral, localProperty, outProjection, primaryConstructor,
propertyDeclaration, value */
