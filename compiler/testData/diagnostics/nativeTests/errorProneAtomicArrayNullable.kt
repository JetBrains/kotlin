// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE

// FILE: K.kt

@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.concurrent.AtomicReference
import kotlin.concurrent.AtomicArray

fun testKotlin() {
    val k = AtomicArray<Int?>(1) { 128 }
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>k.compareAndSet(0, 127, null)<!>

    val l = AtomicReference<Int?>(128)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>l.compareAndSet(128, null)<!>
}
