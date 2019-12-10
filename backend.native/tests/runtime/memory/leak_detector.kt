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
    a.value = a
    val cycles = GC.detectCycles()!!
    assertEquals(1, cycles.size)
    assertTrue(arrayOf(a).contentEquals(GC.findCycle(cycles[0])!!))
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
    val cycles = GC.detectCycles()!!
    assertEquals(1, cycles.size)
    val cycle = GC.findCycle(cycles[0])!!
    assertEquals(32, cycle.size)
    a1.value = null
}

fun main() {
    test1()
    test2()
    test3()
}