// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReferenceArray

fun testJava() {
    val j = AtomicReferenceArray<Int>(127)
    j.compareAndSet(0, 127, 128) // true
    j.compareAndSet(0, 128, 7777) // false

    val jj: AtomicReferenceArray<Int>
    jj = j
}

typealias JavaAtomicReferenceArray<T> = AtomicReferenceArray<T>

fun testTypealiasedJava() {
    val j = JavaAtomicReferenceArray<Int>(127)
    j.compareAndSet(0, 127, 128) // true
    j.compareAndSet(0, 128, 7777) // false

    val jj: JavaAtomicReferenceArray<Int>
    jj = j
}

// FILE: K.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicArray

fun testKotlin() {
    val k = AtomicArray(arrayOf(127))
    k.compareAndSetAt(0, 127, 128) // true
    k.compareAndSetAt(0, 128, 7777) // false

    val kk: AtomicArray<Int>
    kk = k
}

typealias KotlinAtomicArray<T> = AtomicArray<T>

fun testTypealiasedKotlin() {
    val k = KotlinAtomicArray(arrayOf(127))
    k.compareAndSetAt(0, 127, 128) // true
    k.compareAndSetAt(0, 128, 7777) // false

    val kk: KotlinAtomicArray<Int>
    kk = k
}
