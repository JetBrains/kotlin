// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReference

fun testJava() {
    val j = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Int>(127)<!>
    j.compareAndSet(127, 128) // true
    j.compareAndSet(128, 7777) // false

    val jj: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Int><!>
    jj = j
}

typealias JavaAtomicReference<T> = AtomicReference<T>

fun testTypealiasedJava() {
    val j = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>JavaAtomicReference<Int>(127)<!>
    j.compareAndSet(127, 128) // true
    j.compareAndSet(128, 7777) // false

    val jj: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>JavaAtomicReference<Int><!>
    jj = j
}

// FILE: K.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference

fun testKotlin() {
    val k = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Int>(127)<!>
    k.compareAndSet(127, 128) // true
    k.compareAndSet(128, 7777) // false

    val kk: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Int><!>
    kk = k
}

typealias KotlinAtomicReference<T> = AtomicReference<T>

fun testTypealiasedKotlin() {
    val k = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>KotlinAtomicReference<Int>(127)<!>
    k.compareAndSet(127, 128) // true
    k.compareAndSet(128, 7777) // false

    val kk: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>KotlinAtomicReference<Int><!>
    kk = k
}
