// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReference

fun testJava() {
    val j = AtomicReference<Int>(127)
    j.compareAndSet(127, 128) // true
    j.compareAndSet(128, 7777) // false

    val jj: AtomicReference<Int>
    jj = j
}

typealias JavaAtomicReference<T> = AtomicReference<T>

fun testTypealiasedJava() {
    val j = JavaAtomicReference<Int>(127)
    j.compareAndSet(127, 128) // true
    j.compareAndSet(128, 7777) // false

    val jj: JavaAtomicReference<Int>
    jj = j
}

// FILE: K.kt

import kotlin.concurrent.atomics.AtomicReference

fun testKotlin() {
    val k = AtomicReference<Int>(127)
    k.compareAndSet(127, 128) // true
    k.compareAndSet(128, 7777) // false

    val kk: AtomicReference<Int>
    kk = k
}

typealias KotlinAtomicReference<T> = AtomicReference<T>

fun testTypealiasedKotlin() {
    val k = KotlinAtomicReference<Int>(127)
    k.compareAndSet(127, 128) // true
    k.compareAndSet(128, 7777) // false

    val kk: KotlinAtomicReference<Int>
    kk = k
}
