// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReferenceArray

fun testJava() {
    val j = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReferenceArray<Int>(127)<!>
    j.compareAndSet(0, 127, 128) // true
    j.compareAndSet(0, 128, 7777) // false

    val jj: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReferenceArray<Int><!>
    jj = j
}

typealias JavaAtomicReferenceArray<T> = AtomicReferenceArray<T>

fun testTypealiasedJava() {
    val j = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>JavaAtomicReferenceArray<Int>(127)<!>
    j.compareAndSet(0, 127, 128) // true
    j.compareAndSet(0, 128, 7777) // false

    val jj: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>JavaAtomicReferenceArray<Int><!>
    jj = j
}

// FILE: K.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicArray

fun testKotlin() {
    val k = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicArray(arrayOf(127))<!>
    k.compareAndSetAt(0, 127, 128) // true
    k.compareAndSetAt(0, 128, 7777) // false

    val kk: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicArray<Int><!>
    kk = k
}

typealias KotlinAtomicArray<T> = AtomicArray<T>

fun testTypealiasedKotlin() {
    val k = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>KotlinAtomicArray(arrayOf(127))<!>
    k.compareAndSetAt(0, 127, 128) // true
    k.compareAndSetAt(0, 128, 7777) // false

    val kk: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>KotlinAtomicArray<Int><!>
    kk = k
}
