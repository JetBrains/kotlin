// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReference

fun testJava() {
    val j = AtomicReference<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>j.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>127<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>)<!> // true
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>j.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>7777<!>)<!> // false

    val jj: AtomicReference<Int>
    jj = j
}

typealias JavaAtomicReference<T> = AtomicReference<T>

fun testTypealiasedJava() {
    val j = JavaAtomicReference<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>j.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>127<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>)<!> // true
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>j.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>7777<!>)<!> // false

    val jj: JavaAtomicReference<Int>
    jj = j
}

// FILE: K.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference

fun testKotlin() {
    val k = AtomicReference<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>k.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>127<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>)<!> // true
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>k.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>7777<!>)<!> // false

    val kk: AtomicReference<Int>
    kk = k
}

typealias KotlinAtomicReference<T> = AtomicReference<T>

fun testTypealiasedKotlin() {
    val k = KotlinAtomicReference<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>k.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>127<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>)<!> // true
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>k.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>128<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>7777<!>)<!> // false

    val kk: KotlinAtomicReference<Int>
    kk = k
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, assignment, classReference, flexibleType, functionDeclaration,
integerLiteral, javaFunction, localProperty, nullableType, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
