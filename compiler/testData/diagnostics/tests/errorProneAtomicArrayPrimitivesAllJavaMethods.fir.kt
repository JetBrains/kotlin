// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// JDK_KIND: FULL_JDK_21
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIAB

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

fun testReference() {
    val it = AtomicReference<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.compareAndSet(127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.<!DEPRECATION!>weakCompareAndSet<!>(127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.weakCompareAndSetAcquire(127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.weakCompareAndSetRelease(127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.weakCompareAndSetPlain(127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.weakCompareAndSetVolatile(127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.compareAndExchange(127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.compareAndExchangeAcquire(127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.compareAndExchangeRelease(127, 128)<!>
}

fun testReferenceArray() {
    val it = AtomicReferenceArray<Int>(1)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.compareAndSet(0, 127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.<!DEPRECATION!>weakCompareAndSet<!>(0, 127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.weakCompareAndSetAcquire(0, 127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.weakCompareAndSetRelease(0, 127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.weakCompareAndSetPlain(0, 127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.weakCompareAndSetVolatile(0, 127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.compareAndExchange(0, 127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.compareAndExchangeAcquire(0, 127, 128)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>it.compareAndExchangeRelease(0, 127, 128)<!>
}
