// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

// MODULE: common
// FILE: C.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference

fun commonTestKotlin() {
    val k = AtomicReference<Int>(127)
    k.compareAndSet(127, 128) // true
    k.compareAndSet(128, 7777) // false

    val kk: AtomicReference<Int>
    kk = k
}

expect class KotlinAtomicReference<T>(value: T) {
    fun compareAndSet(expectedValue: T, newValue: T): Boolean
}

fun commonTypealiasedKotlin() {
    val k = KotlinAtomicReference<Int>(127)
    k.compareAndSet(127, 128) // true
    k.compareAndSet(128, 7777) // false

    val kk: KotlinAtomicReference<Int>
    kk = k
}

// MODULE: jvm()()(common)
// FILE: K.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference

fun testKotlin() {
    val k = AtomicReference<Int>(127)
    k.compareAndSet(127, 128) // true
    k.compareAndSet(128, 7777) // false

    val kk: AtomicReference<Int>
    kk = k
}

actual typealias KotlinAtomicReference<T> = AtomicReference<T>

fun testTypealiasedKotlin() {
    val k = KotlinAtomicReference<Int>(127)
    k.compareAndSet(127, 128) // true
    k.compareAndSet(128, 7777) // false

    val kk: KotlinAtomicReference<Int>
    kk = k
}

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReference

fun testJava() {
    val j = AtomicReference<Int>(127)
    j.compareAndSet(127, 128) // true
    j.compareAndSet(128, 7777) // false

    val jj: AtomicReference<Int>
    jj = j
}
