/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.concurrent.worker_bound_reference1

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlin.native.internal.GC
import kotlin.native.ref.WeakReference

class A(var a: Int)

val global1: DisposableWorkerBoundReference<A> = DisposableWorkerBoundReference(A(3))

@Test
fun testGlobal() {
    assertEquals(3, global1.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        global1
    }

    val value = future.result
    assertEquals(3, value.value.a)
    worker.requestTermination().result
}

val global2: DisposableWorkerBoundReference<A> = DisposableWorkerBoundReference(A(3))

@Test
fun testGlobalDenyAccessOnWorker() {
    assertEquals(3, global2.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        val local = global2
        assertFailsWith<IncorrectDereferenceException> {
            local.value
        }
        Unit
    }

    future.result
    worker.requestTermination().result
}

val global3: DisposableWorkerBoundReference<A> = DisposableWorkerBoundReference(A(3).freeze())

@Test
fun testGlobalAccessOnWorkerFrozenInitially() {
    assertEquals(3, global3.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        global3.value.a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

val global4: DisposableWorkerBoundReference<A> = DisposableWorkerBoundReference(A(3))

@Test
fun testGlobalAccessOnWorkerFrozenBeforePassing() {
    assertEquals(3, global4.value.a)
    global4.value.freeze()

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        global4.value.a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

val global5: DisposableWorkerBoundReference<A> = DisposableWorkerBoundReference(A(3))

@Test
fun testGlobalAccessOnWorkerFrozenBeforeAccess() {
    val semaphore: AtomicInt = AtomicInt(0)

    assertEquals(3, global5.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { semaphore }) { semaphore ->
        semaphore.increment()
        while (semaphore.value < 2) {
        }

        global5.value.a
    }

    while (semaphore.value < 1) {
    }
    global5.value.freeze()
    semaphore.increment()

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

val global6: DisposableWorkerBoundReference<A> = DisposableWorkerBoundReference(A(3))

@Test
fun testGlobalModification() {
    val semaphore: AtomicInt = AtomicInt(0)

    assertEquals(3, global6.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { semaphore }) { semaphore ->
        semaphore.increment()
        while (semaphore.value < 2) {
        }
        global6
    }

    while (semaphore.value < 1) {
    }
    global6.value.a = 4
    semaphore.increment()

    val value = future.result
    assertEquals(4, value.value.a)
    worker.requestTermination().result
}

val global7: DisposableWorkerBoundReference<A> = DisposableWorkerBoundReference(A(3))

@Test
fun testGlobalDispose() {
    assertEquals(3, global7.value.a)

    global7.dispose()
    global7.dispose()
}

val global8: DisposableWorkerBoundReference<A> = DisposableWorkerBoundReference(A(3))

@Test
fun testGlobalAccessAfterDispose() {
    assertEquals(3, global8.value.a)

    global8.dispose()
    assertFailsWith<IllegalStateException> {
        global8.value.a
    }
}

@Test
fun testLocal() {
    val local = DisposableWorkerBoundReference(A(3))
    assertEquals(3, local.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local
    }

    val value = future.result
    assertEquals(3, value.value.a)
    worker.requestTermination().result
}

@Test
fun testLocalDenyAccessOnWorker() {
    val local = DisposableWorkerBoundReference(A(3))
    assertEquals(3, local.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        assertFailsWith<IncorrectDereferenceException> {
            local.value
        }
        Unit
    }

    future.result
    worker.requestTermination().result
}

@Test
fun testLocalAccessOnWorkerFrozenInitially() {
    val local = DisposableWorkerBoundReference(A(3).freeze())
    assertEquals(3, local.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local.value.a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

@Test
fun testLocalAccessOnWorkerFrozenBeforePassing() {
    val local = DisposableWorkerBoundReference(A(3))
    assertEquals(3, local.value.a)
    local.value.freeze()

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local.value.a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

@Test
fun testLocalAccessOnWorkerFrozenBeforeAccess() {
    val semaphore: AtomicInt = AtomicInt(0)

    val local = DisposableWorkerBoundReference(A(3))
    assertEquals(3, local.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { Pair(local, semaphore) }) { (local, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {
        }

        local.value.a
    }

    while (semaphore.value < 1) {
    }
    local.value.freeze()
    semaphore.increment()

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

@Test
fun testLocalDenyAccessOnMainThread() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        DisposableWorkerBoundReference(A(3))
    }

    val value = future.result
    assertFailsWith<IncorrectDereferenceException> {
        value.value
    }

    worker.requestTermination().result
}

@Test
fun testLocalModification() {
    val semaphore: AtomicInt = AtomicInt(0)

    val local = DisposableWorkerBoundReference(A(3))
    assertEquals(3, local.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { Pair(local, semaphore) }) { (local, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {
        }
        local
    }

    while (semaphore.value < 1) {
    }
    local.value.a = 4
    semaphore.increment()

    val value = future.result
    assertEquals(4, value.value.a)
    worker.requestTermination().result
}

@Test
fun testLocalDispose() {
    val local = DisposableWorkerBoundReference(A(3))
    assertEquals(3, local.value.a)

    local.dispose()
    local.dispose()
}

@Test
fun testLocalAccessAfterDispose() {
    val local = DisposableWorkerBoundReference(A(3))
    assertEquals(3, local.value.a)

    local.dispose()
    assertFailsWith<IllegalStateException> {
        local.value.a
    }
}

fun getOwnerAndWeaks(initial: Int): Triple<AtomicReference<DisposableWorkerBoundReference<A>?>, WeakReference<DisposableWorkerBoundReference<A>>, WeakReference<A>> {
    val ref = DisposableWorkerBoundReference(A(initial))
    val refOwner: AtomicReference<DisposableWorkerBoundReference<A>?> = AtomicReference(ref)
    val refWeak = WeakReference(ref)
    val refValueWeak = WeakReference(ref.value)

    return Triple(refOwner, refWeak, refValueWeak)
}

fun <T : Any> callDispose(ref: AtomicReference<DisposableWorkerBoundReference<T>?>) {
    ref.value!!.dispose()
}

@Test
fun testCollect() {
    val (refOwner, refWeak, refValueWeak) = getOwnerAndWeaks(3)

    refOwner.value = null
    GC.collect()

    // Last reference to DisposableWorkerBoundReference is gone, so it and it's referent are destroyed.
    assertNull(refWeak.value)
    assertNull(refValueWeak.value)
}

@Test
fun testDisposeAndCollect() {
    val (refOwner, refWeak, refValueWeak) = getOwnerAndWeaks(3)

    callDispose(refOwner)
    GC.collect()

    // refOwner still contains a reference to DisposableWorkerBoundReference. But it's referent is
    // destroyed because of explicit dispose call.
    assertNotNull(refWeak.value)
    assertNull(refValueWeak.value)
}

fun collectInWorker(worker: Worker, semaphore: AtomicInt): Pair<WeakReference<A>, Future<Unit>> {
    val (refOwner, _, refValueWeak) = getOwnerAndWeaks(3)

    val future = worker.execute(TransferMode.SAFE, { Pair(refOwner, semaphore) }) { (refOwner, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {
        }

        refOwner.value = null
        GC.collect()
    }

    while (semaphore.value < 1) {
    }
    // At this point worker is spinning on semaphore. refOwner still contains reference to
    // DisposableWorkerBoundReference, so referent is kept alive.
    GC.collect()
    assertNotNull(refValueWeak.value)

    return Pair(refValueWeak, future)
}

@Test
fun testCollectInWorker() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val (refValueWeak, future) = collectInWorker(worker, semaphore)
    semaphore.increment()
    future.result

    // At this point DisposableWorkerBoundReference no longer has a reference, so it's referent is destroyed.
    // DisposableWorkerBoundReference, so referent is kept alive.
    GC.collect()
    assertNull(refValueWeak.value)

    worker.requestTermination().result
}

fun doNotCollectInWorker(worker: Worker, semaphore: AtomicInt): Future<DisposableWorkerBoundReference<A>> {
    val ref = DisposableWorkerBoundReference(A(3))

    return worker.execute(TransferMode.SAFE, { Pair(ref, semaphore) }) { (ref, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {
        }

        GC.collect()
        ref
    }
}

@Test
fun testDoNotCollectInWorker() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val future = doNotCollectInWorker(worker, semaphore)
    while (semaphore.value < 1) {
    }
    GC.collect()
    semaphore.increment()

    val value = future.result
    assertEquals(3, value.value.a)
    worker.requestTermination().result
}

fun disposeInWorker(worker: Worker, semaphore: AtomicInt): Triple<WeakReference<DisposableWorkerBoundReference<A>>, WeakReference<A>, Future<AtomicReference<DisposableWorkerBoundReference<A>?>>> {
    val (refOwner, refWeak, refValueWeak) = getOwnerAndWeaks(3)

    val future = worker.execute(TransferMode.SAFE, { Pair(refOwner, semaphore) }) { (refOwner, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {
        }

        callDispose(refOwner)
        GC.collect()
        refOwner
    }

    while (semaphore.value < 1) {
    }
    // At this point worker is spinning on semaphore. refOwner still contains reference to
    // DisposableWorkerBoundReference, so referent is kept alive.
    GC.collect()
    assertNotNull(refValueWeak.value)

    return Triple(refWeak, refValueWeak, future)
}

@Test
fun testDisposeInWorker() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val (refWeak, refValueWeak, future) = disposeInWorker(worker, semaphore)
    semaphore.increment()
    val refOwner = future.result

    // At this point refOwner still has a reference, but it's explicitly disposed,
    // so referent is destroyed.
    GC.collect()
    assertNotNull(refWeak.value)
    assertNull(refValueWeak.value)

    worker.requestTermination().result
}

@Test
fun testDisposeOnMainThreadAndAccessInWorker() {
    val ref = DisposableWorkerBoundReference(A(3))
    assertEquals(3, ref.value.a)

    ref.dispose()

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { ref }) { ref ->
        var result = 0
        assertFailsWith<IllegalStateException> {
            result = ref.value.a
        }
        result
    }

    val value = future.result
    assertEquals(0, value)
    worker.requestTermination().result
}

@Test
fun testDisposeInWorkerAndAccessOnMainThread() {
    val ref = DisposableWorkerBoundReference(A(3))
    assertEquals(3, ref.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { ref }) { ref ->
        ref.dispose()
    }

    future.result
    assertFailsWith<IllegalStateException> {
        ref.value.a
    }
    worker.requestTermination().result
}

class B1 {
    lateinit var b2: DisposableWorkerBoundReference<B2>
}

data class B2(val b1: DisposableWorkerBoundReference<B1>)

fun createCyclicGarbage(): Triple<AtomicReference<DisposableWorkerBoundReference<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val ref1 = DisposableWorkerBoundReference(B1())
    val ref1Owner: AtomicReference<DisposableWorkerBoundReference<B1>?> = AtomicReference(ref1)
    val ref1Weak = WeakReference(ref1.value)

    val ref2 = DisposableWorkerBoundReference(B2(ref1))
    val ref2Weak = WeakReference(ref2.value)

    ref1.value.b2 = ref2

    return Triple(ref1Owner, ref1Weak, ref2Weak)
}

@Test
fun doesNotCollectCyclicGarbage() {
    val (ref1Owner, ref1Weak, ref2Weak) = createCyclicGarbage()

    ref1Owner.value = null
    GC.collect()

    // If these asserts fail, that means DisposableWorkerBoundReference managed to clean up cyclic garbage all by itself.
    assertNotNull(ref1Weak.value)
    assertNotNull(ref2Weak.value)
}

@Test
fun collectCyclicGarbageWithExplicitDispose() {
    val (ref1Owner, ref1Weak, ref2Weak) = createCyclicGarbage()

    callDispose(ref1Owner)
    GC.collect()

    assertNull(ref1Weak.value)
    assertNull(ref2Weak.value)
}

fun createCrossThreadCyclicGarbage(
        worker: Worker
): Triple<AtomicReference<DisposableWorkerBoundReference<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val ref1 = DisposableWorkerBoundReference(B1())
    val ref1Owner: AtomicReference<DisposableWorkerBoundReference<B1>?> = AtomicReference(ref1)
    val ref1Weak = WeakReference(ref1.value)

    val future = worker.execute(TransferMode.SAFE, { ref1 }) { ref1 ->
        val ref2 = DisposableWorkerBoundReference(B2(ref1))
        Pair(ref2, WeakReference(ref2.value))
    }
    val (ref2, ref2Weak) = future.result

    ref1.value.b2 = ref2

    return Triple(ref1Owner, ref1Weak, ref2Weak)
}

@Test
fun doesNotCollectCrossThreadCyclicGarbage() {
    val worker = Worker.start()

    val (ref1Owner, ref1Weak, ref2Weak) = createCrossThreadCyclicGarbage(worker)

    ref1Owner.value = null
    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result

    // If these asserts fail, that means DisposableWorkerBoundReference managed to clean up cyclic garbage all by itself.
    assertNotNull(ref1Weak.value)
    assertNotNull(ref2Weak.value)

    worker.requestTermination().result
}

@Test
fun collectCrossThreadCyclicGarbageWithExplicitDispose() {
    val worker = Worker.start()

    val (ref1Owner, ref1Weak, ref2Weak) = createCrossThreadCyclicGarbage(worker)

    callDispose(ref1Owner)
    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result

    assertNull(ref1Weak.value)
    assertNull(ref2Weak.value)

    worker.requestTermination().result
}

@Test
fun concurrentAccess() {
    val workerCount = 10
    val workerUnlocker = AtomicInt(0)

    val ref = DisposableWorkerBoundReference(A(3))
    assertEquals(3, ref.value.a)

    val workers = Array(workerCount) {
        Worker.start()
    }
    val futures = Array(workers.size) {
        workers[it].execute(TransferMode.SAFE, { Pair(ref, workerUnlocker) }) { (ref, workerUnlocker) ->
            while (workerUnlocker.value < 1) {
            }

            assertFailsWith<IncorrectDereferenceException> {
                ref.value
            }
            Unit
        }
    }
    workerUnlocker.increment()

    for (future in futures) {
        future.result
    }

    for (worker in workers) {
        worker.requestTermination().result
    }
}

@Test
fun concurrentDispose() {
    val workerCount = 10
    val workerUnlocker = AtomicInt(0)

    val ref = DisposableWorkerBoundReference(A(3))
    assertEquals(3, ref.value.a)

    val workers = Array(workerCount) {
        Worker.start()
    }
    val futures = Array(workers.size) {
        workers[it].execute(TransferMode.SAFE, { Pair(ref, workerUnlocker) }) { (ref, workerUnlocker) ->
            while (workerUnlocker.value < 1) {
            }

            ref.dispose()
        }
    }
    workerUnlocker.increment()

    for (future in futures) {
        future.result
    }

    assertFailsWith<IllegalStateException> {
        ref.value.a
    }

    for (worker in workers) {
        worker.requestTermination().result
    }
}

@Test
fun concurrentDisposeAndAccess() {
    val workerUnlocker = AtomicInt(0)

    val ref = DisposableWorkerBoundReference(A(3))
    assertEquals(3, ref.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { Pair(ref, workerUnlocker) }) { (ref, workerUnlocker) ->
        while (workerUnlocker.value < 1) {
        }

        ref.dispose()
    }
    workerUnlocker.increment()

    var result = 0
    // This is a race, but it should either get value successfully or get IllegalStateException.
    // Any other kind of failure is unacceptable.
    try {
        result = ref.value.a
    } catch (e: IllegalStateException) {
        result = 3
    }
    assertEquals(3, result)

    future.result
    worker.requestTermination().result
}
