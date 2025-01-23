// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE

// FILE: K.kt

@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.concurrent.AtomicReference
import kotlin.concurrent.AtomicArray

fun testKotlin() {
    val k = AtomicArray(1) { 127 }
    k.compareAndSet(0, 127, 128) // true
    k.compareAndSet(0, 128, 7777) // false

    val kk: AtomicArray<Int>
    kk = k

    val l = AtomicReference(127)
    l.compareAndSet(127, 128) // true
    l.compareAndSet(128, 7777) // false

    val ll: AtomicReference<Int>
    ll = l
}
