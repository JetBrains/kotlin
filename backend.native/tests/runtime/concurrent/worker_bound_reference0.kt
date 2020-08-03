/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.concurrent.worker_bound_reference0

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlin.native.internal.GC
import kotlin.native.ref.WeakReference
import kotlin.text.Regex

class A(var a: Int)

@SharedImmutable
val global1: WorkerBoundReference<A> = WorkerBoundReference(A(3))

@Test
fun testGlobal() {
    assertEquals(3, global1.value.a)
    assertEquals(3, global1.valueOrNull?.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        global1
    }

    val value = future.result
    assertEquals(3, value.value.a)
    assertEquals(3, value.valueOrNull?.a)
    worker.requestTermination().result
}

@SharedImmutable
val global2: WorkerBoundReference<A> = WorkerBoundReference(A(3))

@Test
fun testGlobalDenyAccessOnWorker() {
    assertEquals(3, global2.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        val local = global2
        assertFailsWith<IncorrectDereferenceException> {
            local.value
        }
        assertEquals(null, local.valueOrNull)
        Unit
    }

    future.result
    worker.requestTermination().result
}

@SharedImmutable
val global3: WorkerBoundReference<A> = WorkerBoundReference(A(3).freeze())

@Test
fun testGlobalAccessOnWorkerFrozenInitially() {
    assertEquals(3, global3.value.a)
    assertEquals(3, global3.valueOrNull?.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        global3.value.a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

@SharedImmutable
val global4: WorkerBoundReference<A> = WorkerBoundReference(A(3))

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

@SharedImmutable
val global5: WorkerBoundReference<A> = WorkerBoundReference(A(3))

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

@SharedImmutable
val global6: WorkerBoundReference<A> = WorkerBoundReference(A(3))

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
    assertEquals(4, value.valueOrNull?.a)
    worker.requestTermination().result
}

@SharedImmutable
val global7: WorkerBoundReference<A> = WorkerBoundReference(A(3))

@Test
fun testGlobalGetWorker() {
    val ownerId = Worker.current.id
    assertEquals(ownerId, global7.worker.id)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { ownerId }) { ownerId ->
        assertEquals(ownerId, global7.worker.id)
        Unit
    }

    future.result
    worker.requestTermination().result
}

@Test
fun testLocal() {
    val local = WorkerBoundReference(A(3))
    assertEquals(3, local.value.a)
    assertEquals(3, local.valueOrNull?.a)
}

@Test
fun testLocalFrozen() {
    val local = WorkerBoundReference(A(3)).freeze()
    assertEquals(3, local.value.a)
    assertEquals(3, local.valueOrNull?.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local
    }

    val value = future.result
    assertEquals(3, value.value.a)
    assertEquals(3, value.valueOrNull?.a)
    worker.requestTermination().result
}

@Test
fun testLocalDenyAccessOnWorkerFrozen() {
    val local = WorkerBoundReference(A(3)).freeze()
    assertEquals(3, local.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        assertFailsWith<IncorrectDereferenceException> {
            local.value
        }
        assertEquals(null, local.valueOrNull)
        Unit
    }

    future.result
    worker.requestTermination().result
}

@Test
fun testLocalAccessOnWorkerFrozenInitiallyFrozen() {
    val local = WorkerBoundReference(A(3).freeze()).freeze()
    assertEquals(3, local.value.a)
    assertEquals(3, local.valueOrNull?.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local.value.a
    }

    val value = future.result
    assertEquals(3, value)
    worker.requestTermination().result
}

@Test
fun testLocalAccessOnWorkerFrozenBeforePassingFrozen() {
    val local = WorkerBoundReference(A(3)).freeze()
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
fun testLocalAccessOnWorkerFrozenBeforeAccessFrozen() {
    val semaphore: AtomicInt = AtomicInt(0)

    val local = WorkerBoundReference(A(3)).freeze()
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
fun testLocalAccessOnMainThread() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        WorkerBoundReference(A(3))
    }

    assertEquals(3, future.result.value.a)

    worker.requestTermination().result
}

@Test
fun testLocalDenyAccessOnMainThreadFrozen() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        WorkerBoundReference(A(3)).freeze()
    }

    val value = future.result
    assertFailsWith<IncorrectDereferenceException> {
        value.value
    }
    assertEquals(null, value.valueOrNull)

    worker.requestTermination().result
}

@Test
fun testLocalModificationFrozen() {
    val semaphore: AtomicInt = AtomicInt(0)

    val local = WorkerBoundReference(A(3)).freeze()
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
    assertEquals(4, value.valueOrNull?.a)
    worker.requestTermination().result
}

@Test
fun testLocalGetWorkerFrozen() {
    val local = WorkerBoundReference(A(3)).freeze()

    val ownerId = Worker.current.id
    assertEquals(ownerId, local.worker.id)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { Pair(local, ownerId) }) { (local, ownerId) ->
        assertEquals(ownerId, local.worker.id)
        Unit
    }

    future.result
    worker.requestTermination().result
}

@Test
fun testLocalForeignGetWorker() {
    val worker = Worker.start()
    val ownerId = worker.id
    val future = worker.execute(TransferMode.SAFE, { ownerId }) { ownerId ->
        val local = WorkerBoundReference(A(3))
        assertEquals(ownerId, local.worker.id)
        local
    }

    val value = future.result
    assertEquals(ownerId, value.worker.id)

    worker.requestTermination().result
}

@Test
fun testLocalForeignGetWorkerFrozen() {
    val worker = Worker.start()
    val ownerId = worker.id
    val future = worker.execute(TransferMode.SAFE, { ownerId }) { ownerId ->
        val local = WorkerBoundReference(A(3)).freeze()
        assertEquals(ownerId, local.worker.id)
        local
    }

    val value = future.result
    assertEquals(ownerId, value.worker.id)

    worker.requestTermination().result
}

class Wrapper(val ref: WorkerBoundReference<A>)

@Test
fun testLocalWithWrapperFrozen() {
    val local = Wrapper(WorkerBoundReference(A(3))).freeze()
    assertEquals(3, local.ref.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        local
    }

    val value = future.result
    assertEquals(3, value.ref.value.a)
    worker.requestTermination().result
}

@Test
fun testLocalDenyAccessWithWrapperFrozen() {
    val local = Wrapper(WorkerBoundReference(A(3))).freeze()
    assertEquals(3, local.ref.value.a)

    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { local }) { local ->
        assertFailsWith<IncorrectDereferenceException> {
            local.ref.value
        }
        assertEquals(null, local.ref.valueOrNull)
        Unit
    }

    future.result
    worker.requestTermination().result
}

fun getOwnerAndWeaks(initial: Int): Triple<FreezableAtomicReference<WorkerBoundReference<A>?>, WeakReference<WorkerBoundReference<A>>, WeakReference<A>> {
    val ref = WorkerBoundReference(A(initial))
    val refOwner: FreezableAtomicReference<WorkerBoundReference<A>?> = FreezableAtomicReference(ref)
    val refWeak = WeakReference(ref)
    val refValueWeak = WeakReference(ref.value)

    return Triple(refOwner, refWeak, refValueWeak)
}

@Test
fun testCollect() {
    val (refOwner, refWeak, refValueWeak) = getOwnerAndWeaks(3)

    refOwner.value = null
    GC.collect()

    // Last reference to WorkerBoundReference is gone, so it and it's referent are destroyed.
    assertNull(refWeak.value)
    assertNull(refValueWeak.value)
}

fun getOwnerAndWeaksFrozen(initial: Int): Triple<AtomicReference<WorkerBoundReference<A>?>, WeakReference<WorkerBoundReference<A>>, WeakReference<A>> {
    val ref = WorkerBoundReference(A(initial)).freeze()
    val refOwner: AtomicReference<WorkerBoundReference<A>?> = AtomicReference(ref)
    val refWeak = WeakReference(ref)
    val refValueWeak = WeakReference(ref.value)

    return Triple(refOwner, refWeak, refValueWeak)
}

@Test
fun testCollectFrozen() {
    val (refOwner, refWeak, refValueWeak) = getOwnerAndWeaksFrozen(3)

    refOwner.value = null
    GC.collect()

    // Last reference to WorkerBoundReference is gone, so it and it's referent are destroyed.
    assertNull(refWeak.value)
    assertNull(refValueWeak.value)
}

fun collectInWorkerFrozen(worker: Worker, semaphore: AtomicInt): Pair<WeakReference<A>, Future<Unit>> {
    val (refOwner, _, refValueWeak) = getOwnerAndWeaksFrozen(3)

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
    // WorkerBoundReference, so referent is kept alive.
    GC.collect()
    assertNotNull(refValueWeak.value)

    return Pair(refValueWeak, future)
}

@Test
fun testCollectInWorkerFrozen() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val (refValueWeak, future) = collectInWorkerFrozen(worker, semaphore)
    semaphore.increment()
    future.result

    // At this point WorkerBoundReference no longer has a reference, so it's referent is destroyed.
    GC.collect()
    assertNull(refValueWeak.value)

    worker.requestTermination().result
}

fun doNotCollectInWorkerFrozen(worker: Worker, semaphore: AtomicInt): Future<WorkerBoundReference<A>> {
    val ref = WorkerBoundReference(A(3)).freeze()

    return worker.execute(TransferMode.SAFE, { Pair(ref, semaphore) }) { (ref, semaphore) ->
        semaphore.increment()
        while (semaphore.value < 2) {
        }

        GC.collect()
        ref
    }
}

@Test
fun testDoNotCollectInWorkerFrozen() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val future = doNotCollectInWorkerFrozen(worker, semaphore)
    while (semaphore.value < 1) {
    }
    GC.collect()
    semaphore.increment()

    val value = future.result
    assertEquals(3, value.value.a)
    worker.requestTermination().result
}

class B1 {
    lateinit var b2: WorkerBoundReference<B2>
}

data class B2(val b1: WorkerBoundReference<B1>)

fun createCyclicGarbage(): Triple<FreezableAtomicReference<WorkerBoundReference<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val ref1 = WorkerBoundReference(B1())
    val ref1Owner: FreezableAtomicReference<WorkerBoundReference<B1>?> = FreezableAtomicReference(ref1)
    val ref1Weak = WeakReference(ref1.value)

    val ref2 = WorkerBoundReference(B2(ref1))
    val ref2Weak = WeakReference(ref2.value)

    ref1.value.b2 = ref2

    return Triple(ref1Owner, ref1Weak, ref2Weak)
}

@Test
fun collectCyclicGarbage() {
    val (ref1Owner, ref1Weak, ref2Weak) = createCyclicGarbage()

    ref1Owner.value = null
    GC.collect()

    assertNull(ref1Weak.value)
    assertNull(ref2Weak.value)
}

fun createCyclicGarbageFrozen(): Triple<AtomicReference<WorkerBoundReference<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val ref1 = WorkerBoundReference(B1()).freeze()
    val ref1Owner: AtomicReference<WorkerBoundReference<B1>?> = AtomicReference(ref1)
    val ref1Weak = WeakReference(ref1.value)

    val ref2 = WorkerBoundReference(B2(ref1)).freeze()
    val ref2Weak = WeakReference(ref2.value)

    ref1.value.b2 = ref2

    return Triple(ref1Owner, ref1Weak, ref2Weak)
}

@Test
fun doesNotCollectCyclicGarbageFrozen() {
    val (ref1Owner, ref1Weak, ref2Weak) = createCyclicGarbageFrozen()

    ref1Owner.value = null
    GC.collect()

    // If these asserts fail, that means WorkerBoundReference managed to clean up cyclic garbage all by itself.
    assertNotNull(ref1Weak.value)
    assertNotNull(ref2Weak.value)
}

fun createCrossThreadCyclicGarbageFrozen(
        worker: Worker
): Triple<AtomicReference<WorkerBoundReference<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val ref1 = WorkerBoundReference(B1()).freeze()
    val ref1Owner: AtomicReference<WorkerBoundReference<B1>?> = AtomicReference(ref1)
    val ref1Weak = WeakReference(ref1.value)

    val future = worker.execute(TransferMode.SAFE, { ref1 }) { ref1 ->
        val ref2 = WorkerBoundReference(B2(ref1)).freeze()
        Pair(ref2, WeakReference(ref2.value))
    }
    val (ref2, ref2Weak) = future.result

    ref1.value.b2 = ref2

    return Triple(ref1Owner, ref1Weak, ref2Weak)
}

@Test
fun doesNotCollectCrossThreadCyclicGarbageFrozen() {
    val worker = Worker.start()

    val (ref1Owner, ref1Weak, ref2Weak) = createCrossThreadCyclicGarbageFrozen(worker)

    ref1Owner.value = null
    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result

    // If these asserts fail, that means WorkerBoundReference managed to clean up cyclic garbage all by itself.
    assertNotNull(ref1Weak.value)
    assertNotNull(ref2Weak.value)

    worker.requestTermination().result
}

class C1 {
    lateinit var c2: AtomicReference<WorkerBoundReference<C2>?>

    fun dispose() {
        c2.value = null
    }
}

data class C2(val c1: AtomicReference<WorkerBoundReference<C1>>)

fun createCyclicGarbageWithAtomicsFrozen(): Triple<AtomicReference<WorkerBoundReference<C1>?>, WeakReference<C1>, WeakReference<C2>> {
    val ref1 = WorkerBoundReference(C1()).freeze()
    val ref1Weak = WeakReference(ref1.value)

    val ref2 = WorkerBoundReference(C2(AtomicReference(ref1))).freeze()
    val ref2Weak = WeakReference(ref2.value)

    ref1.value.c2 = AtomicReference(ref2)

    return Triple(AtomicReference(ref1), ref1Weak, ref2Weak)
}

fun dispose(refOwner: AtomicReference<WorkerBoundReference<C1>?>) {
    refOwner.value!!.value.dispose()
    refOwner.value = null
}

@Test
fun doesNotCollectCyclicGarbageWithAtomicsFrozen() {
    val (ref1Owner, ref1Weak, ref2Weak) = createCyclicGarbageWithAtomicsFrozen()

    ref1Owner.value = null
    GC.collect()

    // If these asserts fail, that means AtomicReference<WorkerBoundReference> managed to clean up cyclic garbage all by itself.
    assertNotNull(ref1Weak.value)
    assertNotNull(ref2Weak.value)
}

@Test
fun collectCyclicGarbageWithAtomicsFrozen() {
    val (ref1Owner, ref1Weak, ref2Weak) = createCyclicGarbageWithAtomicsFrozen()

    dispose(ref1Owner)
    GC.collect()

    assertNull(ref1Weak.value)
    assertNull(ref2Weak.value)
}

fun createCrossThreadCyclicGarbageWithAtomicsFrozen(
        worker: Worker
): Triple<AtomicReference<WorkerBoundReference<C1>?>, WeakReference<C1>, WeakReference<C2>> {
    val ref1 = WorkerBoundReference(C1()).freeze()
    val ref1Weak = WeakReference(ref1.value)

    val future = worker.execute(TransferMode.SAFE, { ref1 }) { ref1 ->
        val ref2 = WorkerBoundReference(C2(AtomicReference(ref1))).freeze()
        Pair(ref2, WeakReference(ref2.value))
    }
    val (ref2, ref2Weak) = future.result

    ref1.value.c2 = AtomicReference(ref2)

    return Triple(AtomicReference(ref1), ref1Weak, ref2Weak)
}

@Test
fun doesNotCollectCrossThreadCyclicGarbageWithAtomicsFrozen() {
    val worker = Worker.start()

    val (ref1Owner, ref1Weak, ref2Weak) = createCrossThreadCyclicGarbageWithAtomicsFrozen(worker)

    ref1Owner.value = null
    GC.collect()
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result

    // If these asserts fail, that means AtomicReference<WorkerBoundReference> managed to clean up cyclic garbage all by itself.
    assertNotNull(ref1Weak.value)
    assertNotNull(ref2Weak.value)

    worker.requestTermination().result
}

@Test
fun collectCrossThreadCyclicGarbageWithAtomicsFrozen() {
    val worker = Worker.start()

    val (ref1Owner, ref1Weak, ref2Weak) = createCrossThreadCyclicGarbageWithAtomicsFrozen(worker)

    dispose(ref1Owner)
    // This marks C2 as gone on the main thread
    GC.collect()
    // This cleans up all the references from the worker thread and destroys C2, but C1 is still alive.
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result
    // And this finally destroys C1
    GC.collect()

    assertNull(ref1Weak.value)
    assertNull(ref2Weak.value)

    worker.requestTermination().result
}

@Test
fun concurrentAccessFrozen() {
    val workerCount = 10
    val workerUnlocker = AtomicInt(0)

    val ref = WorkerBoundReference(A(3)).freeze()
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
fun testExceptionMessageFrozen() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        WorkerBoundReference(A(3)).freeze()
    }
    val value = future.result

    val ownerName = worker.name
    val messagePattern = Regex("illegal attempt to access non-shared runtime\\.concurrent\\.worker_bound_reference0\\.A@[a-f0-9]+ bound to `$ownerName` from `${Worker.current.name}`")

    val exception = assertFailsWith<IncorrectDereferenceException> {
        value.value
    }
    assertTrue(messagePattern matches exception.message!!)

    worker.requestTermination().result
}

@Test
fun testDoubleFreeze() {
    val ref = WorkerBoundReference(A(3))
    val wrapper = Wrapper(ref)
    ref.freeze()
    ref.freeze()
    wrapper.freeze()
}

@Test
fun testDoubleFreezeWithFreezeBlocker() {
    val ref = WorkerBoundReference(A(3))
    val wrapper = Wrapper(ref)
    wrapper.ensureNeverFrozen()
    assertFailsWith<FreezingException> {
        wrapper.freeze()
    }
    ref.freeze()
}
