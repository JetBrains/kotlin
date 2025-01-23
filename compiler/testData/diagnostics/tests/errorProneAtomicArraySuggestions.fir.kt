// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.AtomicReference

fun testJavaReference() {
    val ji = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Int>(1)<!>
    val jji: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Int><!>
    jji = ji

    val jl = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Long>(1L)<!>
    val jjl: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Long><!>
    jjl = jl

    val jb = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Char>('c')<!>
    val jjb: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Char><!>
    jjb = jb
}

fun testJavaArray() {
    val ji = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReferenceArray<Int>(1)<!>
    val jji: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReferenceArray<Int><!>
    jji = ji

    val jl = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReferenceArray<Long>(1)<!>
    val jjl: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReferenceArray<Long><!>
    jjl = jl

    val jb = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReferenceArray<Boolean>(1)<!>
    val jjb: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReferenceArray<Boolean><!>
    jjb = jb
}

// FILE: K.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.AtomicArray

fun testKotlinReference() {
    val ki = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference(127)<!>
    val kki: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Int><!>
    kki = ki

    val kl = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference(127L)<!>
    val kkl: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Long><!>
    kkl = kl

    val kb = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference(false)<!>
    val kkb: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicReference<Boolean><!>
    kkb = kb
}

fun testKotlinArray() {
    val ki = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicArray(arrayOf(127))<!>
    val kki: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicArray<Int><!>
    kki = ki

    val kl = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicArray(arrayOf(127L))<!>
    val kkl: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicArray<Long><!>
    kkl = kl

    val kb = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicArray(arrayOf(false))<!>
    val kkb: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicArray<Boolean><!>
    kkb = kb
}
