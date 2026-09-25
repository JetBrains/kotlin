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
    synchronized(t) {}
    WeakReference(t)
    System.identityHashCode(t)
    IdentityHashMap<T, Any>()
}

fun <T : <!FINAL_UPPER_BOUND!>FullVal<!>> boundedByFullValueClass(t: T) {
    synchronized(t) {}
    WeakReference(t)
}

fun <T : AbstractVal> boundedByAbstractValueClass(t: T) {
    synchronized(t) {}
    WeakReference(t)
}

fun <T : <!FINAL_UPPER_BOUND!>Int<!>> boundedByPrimitive(t: T) {
    synchronized(t) {}
    WeakReference(t)
}

fun <T : Int?> boundedByNullablePrimitive(t: T & Any) {
    synchronized(t) {}
}

fun <T : <!FINAL_UPPER_BOUND!>InlineVal<!>, U : T> boundedByTypeParameter(u: U) {
    synchronized(u) {}
}

fun captured(inlines: MutableList<out InlineVal>, fulls: MutableList<out FullVal>, ints: MutableList<out Int>) {
    synchronized(inlines[0]) {}
    synchronized(fulls[0]) {}
    synchronized(ints[0]) {}
    WeakReference(inlines[0])
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, dnnType, flexibleType, functionDeclaration, integerLiteral,
javaFunction, lambdaLiteral, nullableType, outProjection, primaryConstructor, propertyDeclaration, typeConstraint,
typeParameter, value */
