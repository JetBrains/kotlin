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
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

value class Full(val x: Int, val y: Int)

@JvmInline
value class Inline(val x: Int)

fun javaAtomics(full: AtomicReference<Full>, inline: AtomicReference<Inline>, int: AtomicReference<Int?>, array: AtomicReferenceArray<Full>) {
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>full.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Full(1, 2)<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Full(3, 4)<!>)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>inline.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Inline(1)<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Inline(2)<!>)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>int.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>1<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>2<!>)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>array.compareAndSet(0, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Full(1, 2)<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Full(3, 4)<!>)<!>
}

fun <T : <!FINAL_UPPER_BOUND!>Full<!>> bound(ref: AtomicReference<T>, t: T) {
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>ref.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>)<!>
}

fun kotlinAtomics(full: kotlin.concurrent.atomics.AtomicReference<Full>) {
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>full.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Full(1, 2)<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Full(3, 4)<!>)<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, primaryConstructor,
propertyDeclaration, typeConstraint, typeParameter, value */
