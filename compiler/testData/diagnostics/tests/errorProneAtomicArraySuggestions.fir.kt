// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.AtomicReference

fun testJavaReference() {
    val ji = AtomicReference<Int>(1)
    val jji: AtomicReference<Int>
    jji = ji
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>ji.compareAndSet(1, 2)<!>

    val jl = AtomicReference<Long>(1L)
    val jjl: AtomicReference<Long>
    jjl = jl
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>jl.compareAndSet(1L, 2L)<!>

    val jb = AtomicReference<Char>('c')
    val jjb: AtomicReference<Char>
    jjb = jb
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>jb.compareAndSet('c', 'd')<!>
}

fun testJavaArray() {
    val ji = AtomicReferenceArray<Int>(1)
    val jji: AtomicReferenceArray<Int>
    jji = ji
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>ji.compareAndSet(0, 1, 2)<!>

    val jl = AtomicReferenceArray<Long>(1)
    val jjl: AtomicReferenceArray<Long>
    jjl = jl
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>jl.compareAndSet(0, 1L, 2L)<!>

    val jb = AtomicReferenceArray<Boolean>(1)
    val jjb: AtomicReferenceArray<Boolean>
    jjb = jb
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>jb.compareAndSet(0, true, false)<!>
}

// FILE: K.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.AtomicArray

fun testKotlinReference() {
    val ki = AtomicReference(127)
    val kki: AtomicReference<Int>
    kki = ki
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>ki.compareAndSet(127, 2)<!>

    val kl = AtomicReference(127L)
    val kkl: AtomicReference<Long>
    kkl = kl
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>kl.compareAndSet(127L, 2L)<!>

    val kb = AtomicReference(false)
    val kkb: AtomicReference<Boolean>
    kkb = kb
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>kb.compareAndSet(false, true)<!>
}

fun testKotlinArray() {
    val ki = AtomicArray(arrayOf(127))
    val kki: AtomicArray<Int>
    kki = ki
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>ki.compareAndSetAt(0, 127, 2)<!>

    val kl = AtomicArray(arrayOf(127L))
    val kkl: AtomicArray<Long>
    kkl = kl
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>kl.compareAndSetAt(0, 127L, 2L)<!>

    val kb = AtomicArray(arrayOf(false))
    val kkb: AtomicArray<Boolean>
    kkb = kb
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>kb.compareAndSetAt(0, false, true)<!>
}
