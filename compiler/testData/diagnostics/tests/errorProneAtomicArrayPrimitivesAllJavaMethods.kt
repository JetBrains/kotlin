// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// JDK_KIND: FULL_JDK_21
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIAB

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

fun testReference() {
    val it = AtomicReference<Int>(127)
    it.compareAndSet(127, 128)
    it.<!DEPRECATION!>weakCompareAndSet<!>(127, 128)
    it.weakCompareAndSetAcquire(127, 128)
    it.weakCompareAndSetRelease(127, 128)
    it.weakCompareAndSetPlain(127, 128)
    it.weakCompareAndSetVolatile(127, 128)
    it.compareAndExchange(127, 128)
    it.compareAndExchangeAcquire(127, 128)
    it.compareAndExchangeRelease(127, 128)
}

fun testReferenceArray() {
    val it = AtomicReferenceArray<Int>(1)
    it.compareAndSet(0, 127, 128)
    it.<!DEPRECATION!>weakCompareAndSet<!>(0, 127, 128)
    it.weakCompareAndSetAcquire(0, 127, 128)
    it.weakCompareAndSetRelease(0, 127, 128)
    it.weakCompareAndSetPlain(0, 127, 128)
    it.weakCompareAndSetVolatile(0, 127, 128)
    it.compareAndExchange(0, 127, 128)
    it.compareAndExchangeAcquire(0, 127, 128)
    it.compareAndExchangeRelease(0, 127, 128)
}
