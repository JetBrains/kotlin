// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: +FullValueClasses
// JDK_KIND: FULL_JDK_21

import java.lang.ref.WeakReference
import java.util.IdentityHashMap

@JvmInline
value class InlineVal(val x: Int)

value class FullVal(val x: Int)

abstract value class AbstractVal

fun <T : <!FINAL_UPPER_BOUND!>InlineVal<!>> boundedByInlineValueClass(t: T) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>t<!>) {}
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>t<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>t<!>)
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>T<!>, Any>()
}

fun <T : <!FINAL_UPPER_BOUND!>FullVal<!>> boundedByFullValueClass(t: T) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>t<!>) {}
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>t<!>)
}

fun <T : AbstractVal> boundedByAbstractValueClass(t: T) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>t<!>) {}
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>t<!>)
}

fun <T : <!FINAL_UPPER_BOUND!>Int<!>> boundedByPrimitive(t: T) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>t<!>) {}
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>t<!>)
}

fun <T : Int?> boundedByNullablePrimitive(t: T & Any) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>t<!>) {}
}

fun <T : <!FINAL_UPPER_BOUND!>InlineVal<!>, U : T> boundedByTypeParameter(u: U) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>u<!>) {}
}

fun captured(inlines: MutableList<out InlineVal>, fulls: MutableList<out FullVal>, ints: MutableList<out Int>) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>inlines[0]<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>fulls[0]<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>ints[0]<!>) {}
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inlines[0]<!>)
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, dnnType, flexibleType, functionDeclaration, integerLiteral,
javaFunction, lambdaLiteral, nullableType, outProjection, primaryConstructor, propertyDeclaration, typeConstraint,
typeParameter, value */
