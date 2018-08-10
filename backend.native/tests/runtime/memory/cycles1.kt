package runtime.memory.cycles1

import kotlin.test.*
import kotlin.native.ref.*

@Test fun runTest() {
    val weakRefToTrashCycle = createLoop()
    kotlin.native.internal.GC.collect()
    assertNull(weakRefToTrashCycle.get())
}

private fun createLoop(): WeakReference<Any> {
    val loop = Array<Any?>(1, { null })
    loop[0] = loop

    return WeakReference(loop)
}