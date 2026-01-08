// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-77113

// FILE: Kotlin.kt
@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.AtomicArray

fun testKotlin() {
    val b = AtomicReference<Int?>(128)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>b.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>, null)<!>

    val c = AtomicArray(arrayOf(128, null))
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>c.compareAndSetAt(0, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>, null)<!>
}

// FILE: Java.kt
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

fun testJava() {
    val b = AtomicReference<Int?>(128)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>b.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>, null)<!>

    val c = AtomicReferenceArray<Int?>(1)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>c.compareAndSet(0, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>, null)<!>
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, flexibleType, functionDeclaration, integerLiteral,
javaFunction, localProperty, nullableType, propertyDeclaration */
