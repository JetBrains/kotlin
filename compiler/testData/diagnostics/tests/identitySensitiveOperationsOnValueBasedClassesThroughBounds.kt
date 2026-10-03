// RUN_PIPELINE_TILL: CODEGEN
// TARGET_BACKEND: JVM
// WITH_STDLIB
// JDK version is important, because we rely on @ValueBased annotation being present on ZoneId and ProcessHandle
// JDK_KIND: FULL_JDK_21

import java.lang.ref.WeakReference
import java.time.ZoneId
import java.util.IdentityHashMap

fun <T : ZoneId> boundedByValueBasedClass(t: T, other: T) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>t<!>) {}
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>t<!> === <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>other<!>
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>t<!>)
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>t<!>)
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>T<!>, Any>()
}

fun <T : ProcessHandle> boundedByValueBasedInterface(t: T) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>t<!>) {}
}

fun <T : ZoneId, U : T> boundedByTypeParameter(u: U) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>u<!>) {}
}

fun <T : ZoneId?> boundedByNullableValueBasedClass(t: T & Any) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>t<!>) {}
}

fun captured(zones: MutableList<out ZoneId>) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>zones[0]<!>) {}
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>zones[0]<!>)
}

/* GENERATED_FIR_TAGS: capturedType, dnnType, equalityExpression, flexibleType, functionDeclaration, integerLiteral,
javaFunction, lambdaLiteral, nullableType, outProjection, typeConstraint, typeParameter */
