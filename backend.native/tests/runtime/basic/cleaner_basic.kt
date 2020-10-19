/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalTime::class, ExperimentalStdlibApi::class)

package runtime.basic.cleaner_basic

import kotlin.test.*

import kotlin.native.internal.*
import kotlin.native.concurrent.*
import kotlin.native.ref.WeakReference
import kotlin.time.*

class AtomicBoolean(initialValue: Boolean) {
    private val impl = AtomicInt(if (initialValue) 1 else 0)

    init {
        freeze()
    }

    public var value: Boolean
        get() = impl.value != 0
        set(new) { impl.value = if (new) 1 else 0 }
}

class FunBox(private val impl: () -> Unit) {
    fun call() {
        impl()
    }
}

@Test
fun testCleanerLambda() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = {
            val funBox = FunBox { called.value = true }.freeze()
            funBoxWeak = WeakReference(funBox)
            createCleaner(funBox) { it.call() }
        }()
        GC.collect()  // Make sure local funBox reference is gone
        cleanerWeak = WeakReference(cleaner)
        assertFalse(called.value)
    }()

    GC.collect()
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

@Test
fun testCleanerAnonymousFunction() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = {
            val funBox = FunBox { called.value = true }.freeze()
            funBoxWeak = WeakReference(funBox)
            createCleaner(funBox, fun (it: FunBox) { it.call() })
        }()
        GC.collect()  // Make sure local funBox reference is gone
        cleanerWeak = WeakReference(cleaner)
        assertFalse(called.value)
    }()

    GC.collect()
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

@Test
fun testCleanerFunctionReference() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = {
            val funBox = FunBox { called.value = true }.freeze()
            funBoxWeak = WeakReference(funBox)
            createCleaner(funBox, FunBox::call)
        }()
        GC.collect()  // Make sure local funBox reference is gone
        cleanerWeak = WeakReference(cleaner)
        assertFalse(called.value)
    }()

    GC.collect()
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

@Test
fun testCleanerFailWithNonShareableArgument() {
    val funBox = FunBox {}
    assertFailsWith<IllegalArgumentException> {
        createCleaner(funBox) {}
    }
}

inline fun tryWithTimeout(timeoutSeconds: Int, f: () -> Unit): Unit {
    val timeout = TimeSource.Monotonic.markNow() + timeoutSeconds.seconds
    while (true) {
        try {
            f()
            return
        } catch (e: Throwable) {
            if (timeout.hasPassedNow()) {
                throw e
            }
        }
    }
}

@Test
fun testCleanerCleansWithoutGC() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = {
            val funBox = FunBox { called.value = true }.freeze()
            funBoxWeak = WeakReference(funBox)
            createCleaner(funBox) { it.call() }
        }()
        GC.collect()  // Make sure local funBox reference is gone
        cleaner.freeze()
        cleanerWeak = WeakReference(cleaner)
        assertFalse(called.value)
    }()

    GC.collect()

    assertNull(cleanerWeak!!.value)
    tryWithTimeout(3) {
        GC.collect()  // Collect local stack (from previous iteration)
        assertTrue(called.value)
        // If this fails, GC has somehow ran on the cleaners worker.
        assertNotNull(funBoxWeak!!.value)
    }
}

val globalInt = AtomicInt(0)

@Test
fun testCleanerWithInt() {
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = createCleaner(42) {
            globalInt.value = it
        }.freeze()
        cleanerWeak = WeakReference(cleaner)
        assertEquals(0, globalInt.value)
    }()

    GC.collect()
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertEquals(42, globalInt.value)
}

val globalPtr = AtomicNativePtr(NativePtr.NULL)

@Test
fun testCleanerWithNativePtr() {
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = createCleaner(NativePtr.NULL + 42L) {
            globalPtr.value = it
        }
        cleanerWeak = WeakReference(cleaner)
        assertEquals(NativePtr.NULL, globalPtr.value)
    }()

    GC.collect()
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertEquals(NativePtr.NULL + 42L, globalPtr.value)
}

@Test
fun testCleanerWithException() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val funBox = FunBox { called.value = true }.freeze()
        funBoxWeak = WeakReference(funBox)
        val cleaner = createCleaner(funBox) {
            it.call()
            error("Cleaner block failed")
        }
        cleanerWeak = WeakReference(cleaner)
    }()

    GC.collect()
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    // Cleaners block started executing.
    assertTrue(called.value)
    // Even though the block failed, the captured funBox is freed.
    assertNull(funBoxWeak!!.value)
}
