import kotlin.native.concurrent.*
import kotlin.native.internal.GC
import kotlin.test.*

/*
 * Typical snippet for the leak detector usage.
 */
fun dumpLeaks() {
    GC.collect()
    GC.detectCycles()?.let { cycles ->
        cycles.firstOrNull()?.let { root ->
            val cycle = GC.findCycle(root)
            println(cycle?.contentToString())
        }
    }
}

fun test1() {
    val a = AtomicReference<Any?>(null)
    val b = AtomicReference<Any?>(null)
    a.value = b
    b.value = a
    val cycles = GC.detectCycles()!!
    assertEquals(1, cycles.size)
    val cycle = GC.findCycle(cycles[0])!!
    assertEquals(2, cycle.size)
    assertTrue(cycle.contains(a))
    assertTrue(cycle.contains(b))
    a.value = null
}

class Holder(var other: Any?)

fun test2() {
    val array = arrayOf(AtomicReference<Any?>(null), AtomicReference<Any?>(null))
    val obj1 = Holder(array).freeze()
    array[0].value = obj1
    val cycles = GC.detectCycles()!!
    assertEquals(1, cycles.size)
    assertTrue(arrayOf(obj1, array, array[0]).contentEquals(GC.findCycle(cycles[0])!!))
    array[0].value = null
}

fun test3() {
    val a1 = FreezableAtomicReference<Any?>(null)
    val head = Holder(null)
    var current = head
    repeat(30) {
        val next = Holder(null)
        current.other = next
        current = next
    }
    a1.value = head
    current.other = a1
    current.freeze()
    val cycles = GC.detectCycles()!!
    assertEquals(1, cycles.size)
    val cycle = GC.findCycle(cycles[0])!!
    assertEquals(32, cycle.size)
    a1.value = null
}


fun test4() {
    val atomic = AtomicReference<Any?>(null)
    atomic.value = Pair(atomic, Holder(atomic)).freeze()
}

fun main() {
    // We must disable cyclic collector here, to avoid interfering with cycle detector.
    kotlin.native.internal.GC.cyclicCollectorEnabled = false
    /*test1()
    test2()
    test3() */
    test4()
    kotlin.native.internal.GC.cyclicCollectorEnabled = true
}