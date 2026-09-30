// RUN_PIPELINE_TILL: CODEGEN
// TARGET_BACKEND: JVM
// WITH_STDLIB
// JDK version is important, because we rely on @ValueBased annotation being present on ZoneId and ProcessHandle
// JDK_KIND: FULL_JDK_21

import java.lang.ref.WeakReference
import java.time.ZoneId
import java.util.IdentityHashMap

fun <T : ZoneId> boundedByValueBasedClass(t: T, other: T) {
    synchronized(t) {}
    t === other
    WeakReference(t)
    System.identityHashCode(t)
    IdentityHashMap<T, Any>()
}

fun <T : ProcessHandle> boundedByValueBasedInterface(t: T) {
    synchronized(t) {}
}

fun <T : ZoneId, U : T> boundedByTypeParameter(u: U) {
    synchronized(u) {}
}

fun <T : ZoneId?> boundedByNullableValueBasedClass(t: T & Any) {
    synchronized(t) {}
}

fun captured(zones: MutableList<out ZoneId>) {
    synchronized(zones[0]) {}
    WeakReference(zones[0])
}

/* GENERATED_FIR_TAGS: capturedType, dnnType, equalityExpression, flexibleType, functionDeclaration, integerLiteral,
javaFunction, lambdaLiteral, nullableType, outProjection, typeConstraint, typeParameter */
