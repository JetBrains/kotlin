/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalTime::class, ExperimentalStdlibApi::class)

package runtime.basic.cleaner_workers

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
fun testCleanerDestroyInChild() {
    val worker = Worker.start()

    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    worker.execute(TransferMode.SAFE, {
        val funBox = FunBox { called.value = true }.freeze()
        funBoxWeak = WeakReference(funBox)
        val cleaner = createCleaner(funBox) { it.call() }
        cleanerWeak = WeakReference(cleaner)
        Pair(called, cleaner)
    }) { (called, cleaner) ->
        assertFalse(called.value)
    }.result

    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)

    worker.requestTermination().result
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
fun testCleanerDestroyWithChild() {
    val worker = Worker.start()

    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    worker.execute(TransferMode.SAFE, {
        val funBox = FunBox { called.value = true }.freeze()
        funBoxWeak = WeakReference(funBox)
        val cleaner = createCleaner(funBox) { it.call() }
        cleanerWeak = WeakReference(cleaner)
        Pair(called, cleaner)
    }) { (called, cleaner) ->
        assertFalse(called.value)
    }.result

    GC.collect()
    worker.requestTermination().result

    tryWithTimeout(3) {
        GC.collect()  // Collect local stack (from previous iteration)
        performGCOnCleanerWorker()  // Collect cleaners stack

        assertNull(cleanerWeak!!.value)
        assertTrue(called.value)
        assertNull(funBoxWeak!!.value)
    }
}

@Test
fun testCleanerDestroyFrozenWithChild() {
    val worker = Worker.start()

    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    worker.execute(TransferMode.SAFE, {
        val funBox = FunBox { called.value = true }.freeze()
        funBoxWeak = WeakReference(funBox)
        val cleaner = createCleaner(funBox) { it.call() }.freeze()
        cleanerWeak = WeakReference(cleaner)
        Pair(called, cleaner)
    }) { (called, cleaner) ->
        assertFalse(called.value)
    }.result

    GC.collect()
    worker.requestTermination().result

    tryWithTimeout(3) {
        GC.collect()  // Collect local stack (from previous iteration)
        performGCOnCleanerWorker()  // Collect cleaners stack

        assertNull(cleanerWeak!!.value)
        assertTrue(called.value)
        assertNull(funBoxWeak!!.value)
    }
}

@Test
fun testCleanerDestroyFrozenInChild() {
    val worker = Worker.start()

    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        worker.execute(TransferMode.SAFE, {
            val funBox = FunBox { called.value = true }.freeze()
            funBoxWeak = WeakReference(funBox)
            val cleaner = createCleaner(funBox) { it.call() }.freeze()
            cleanerWeak = WeakReference(cleaner)
            Pair(called, cleaner)
        }) { (called, cleaner) ->
            assertFalse(called.value)
        }.result
    }()

    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)

    worker.requestTermination().result
}

@Test
fun testCleanerDestroyInMain() {
    val worker = Worker.start()

    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val result = worker.execute(TransferMode.SAFE, { called }) { called ->
            val funBox = FunBox { called.value = true }.freeze()
            val cleaner = createCleaner(funBox) { it.call() }
            Triple(cleaner, WeakReference(funBox), WeakReference(cleaner))
        }.result
        val cleaner = result.first
        funBoxWeak = result.second
        cleanerWeak = result.third
        assertFalse(called.value)
    }()

    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)

    worker.requestTermination().result
}

@Test
fun testCleanerDestroyFrozenInMain() {
    val worker = Worker.start()

    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val result = worker.execute(TransferMode.SAFE, { called }) { called ->
            val funBox = FunBox { called.value = true }.freeze()
            val cleaner = createCleaner(funBox) { it.call() }.freeze()
            Triple(cleaner, WeakReference(funBox), WeakReference(cleaner))
        }.result
        val cleaner = result.first
        funBoxWeak = result.second
        cleanerWeak = result.third
        assertFalse(called.value)
    }()

    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)

    worker.requestTermination().result
}

@Test
fun testCleanerDestroyShared() {
    val worker = Worker.start()

    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    val cleanerHolder: AtomicReference<Cleaner?> = AtomicReference(null);
    {
        val funBox = FunBox { called.value = true }.freeze()
        funBoxWeak = WeakReference(funBox)
        val cleaner = createCleaner(funBox) { it.call() }.freeze()
        cleanerWeak = WeakReference(cleaner)
        cleanerHolder.value = cleaner
        worker.execute(TransferMode.SAFE, { Pair(called, cleanerHolder) }) { (called, cleanerHolder) ->
            cleanerHolder.value = null
            assertFalse(called.value)
        }.result
    }()

    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)

    worker.requestTermination().result
}

@ThreadLocal
var tlsValue = 11

@Test
fun testCleanerWithTLS() {
    val worker = Worker.start()

    tlsValue = 12

    val value = AtomicInt(0)
    worker.execute(TransferMode.SAFE, {value}) {
        tlsValue = 13
        createCleaner(it) {
            it.value = tlsValue
        }
        Unit
    }.result

    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result
    performGCOnCleanerWorker()

    assertEquals(11, value.value)

    worker.requestTermination().result
}
