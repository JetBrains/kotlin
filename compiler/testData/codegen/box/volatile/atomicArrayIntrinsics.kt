// TARGET_BACKEND: NATIVE

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import kotlin.native.concurrent.*
import kotlin.concurrent.*
import kotlin.native.internal.*
import kotlin.test.*

private val intArrStatic = IntArray(10) { i: Int -> i * 10 }
private val longArrStatic = LongArray(10) { i: Int -> i * 10L }
private val refArrStatic = arrayOfNulls<String?>(10)

private class ArrayIntrinsicsSmokeTest {
    val intArr = IntArray(10) { i: Int -> i * 10 }
    val longArr = LongArray(10) { i: Int -> i * 10L }
    val refArr = arrayOfNulls<String?>(10)

    fun testIntArrayIntrinsics() {
        // atomicGet
        val res = intArr.atomicGet(2)
        assertEquals(20, res, "IntArray: FAIL 1")
        // compareAndExchange
        val res1 = intArr.compareAndExchange(2, 20, 222) // success
        assertTrue(res1 == 20 && intArr[2] == 222, "IntArray: FAIL 2")
        val res2 = intArr.compareAndExchange(2, 222, 2222) // success
        assertTrue(res2 == 222 && intArr[2] == 2222, "IntArray: FAIL 3")
        val res3 = intArr.compareAndExchange(2, 223, 22222) // should fail
        assertTrue(res3 == 2222 && intArr[2] == 2222, "IntArray: FAIL 4")
        val res4 = intArr.compareAndExchange(9, 10, 999) // should fail
        assertTrue(res4 == 90 && intArr[9] == 90, "IntArray: FAIL 5")
        // getAndSet
        assertEquals(2222, intArr.getAndSet(2, 20), "IntArray: FAIL 6")
        assertEquals(20, intArr.getAndSet(2, 200), "IntArray: FAIL 7")
        assertEquals(200, intArr.atomicGet(2), "IntArray: FAIL 8")
        // atomicSet
        intArr.atomicSet(1, 111)
        assertEquals(111, intArr.atomicGet(1), "IntArray: FAIL 9")
        // getAndAdd
        assertEquals(111, intArr.getAndAdd(1, 100), "IntArray: FAIL 10")
        assertEquals(211, intArr.atomicGet(1), "IntArray: FAIL 11")
        assertEquals(211, intArr.getAndAdd(1, -100), "IntArray: FAIL 12")
        assertEquals(111, intArr.atomicGet(1), "IntArray: FAIL 13")
        // compareAndSet
        val aaa = 1111
        intArr.atomicSet(1, aaa)
        assertTrue(intArr.compareAndSet(1, 1111, 2222), "IntArray: FAIL 14")
        assertFalse(intArr.compareAndSet(1, 1111, 3333), "IntArray: FAIL 15")
        assertTrue(intArr.atomicGet(1) == 2222, "IntArray: FAIL 16")
    }

    fun testLongArrayIntrinsics() {
        // atomicGet
        val res = longArr.atomicGet(2)
        assertEquals(20L, res, "IntArray: FAIL 1")
        // compareAndExchange
        val res1 = longArr.compareAndExchange(2, 20L, 222L) // success
        assertTrue(res1 == 20L && longArr[2] == 222L, "LongArray: FAIL 2")
        val res2 = longArr.compareAndExchange(2, 222L, 2222L) // success
        assertTrue(res2 == 222L && longArr[2] == 2222L, "LongArray: FAIL 3")
        val res3 = longArr.compareAndExchange(2, 223L, 22222L) // should fail
        assertTrue(res3 == 2222L && longArr[2] == 2222L, "LongArray: FAIL 4")
        val res4 = longArr.compareAndExchange(9, 10L, 999L) // should fail
        assertTrue(res4 == 90L && longArr[9] == 90L, "LongArray: FAIL 5")
        // getAndSet
        assertEquals(2222L, longArr.getAndSet(2, 20L), "LongArray: FAIL 6")
        assertEquals(20L, longArr.getAndSet(2, 200L), "LongArray: FAIL 7")
        assertEquals(200L, longArr.atomicGet(2), "LongArray: FAIL 8")
        // atomicSet
        longArr.atomicSet(1, 111L)
        assertEquals(111L, longArr.atomicGet(1), "LongArray: FAIL 9")
        // getAndAdd
        assertEquals(111L, longArr.getAndAdd(1, 100L), "LongArray: FAIL 10")
        assertEquals(211L, longArr.atomicGet(1), "LongArray: FAIL 11")
        assertEquals(211L, longArr.getAndAdd(1, -100L), "LongArray: FAIL 12")
        assertEquals(111L, longArr.atomicGet(1), "LongArray: FAIL 13")
        // compareAndSet
        val aaa = 1111L
        longArr.atomicSet(1, aaa)
        assertTrue(longArr.compareAndSet(1, 1111L, 2222L), "LongArray: FAIL 14")
        assertFalse(longArr.compareAndSet(1, 1111L, 3333L), "LongArray: FAIL 15")
        assertTrue(longArr.atomicGet(1) == 2222L, "LongArray: FAIL 16")
    }

    fun testReferenceArrayIntrinsics() {
        // atomicGet
        assertTrue(refArr.atomicGet(0) == refArr[0], "Array: FAIL 1")
        // compareAndExchange
        val newValue = "aaa"
        val res1 = refArr.compareAndExchange(3, null, newValue)
        assertTrue(res1 == null && refArr[3] == newValue, "Array: FAIL 2")
        val res2 = refArr.compareAndExchange(3, newValue, "bbb")
        assertTrue(res2 == newValue && refArr[3] == "bbb", "Array: FAIL 3")
        val res3 = refArr.compareAndExchange(3, newValue, "ccc")
        assertTrue(res3 == "bbb" && refArr[3] == "bbb", "Array: FAIL 4")
        // getAndSet
        val res4 = refArr.getAndSet(4, "aaa")
        assertEquals(null, res4, "Array: FAIL 5")
        val res5 = refArr.getAndSet(4, "bbb")
        assertEquals("aaa", res5, "Array: FAIL 6")
        //set
        refArr.atomicSet(1, "aaa")
        assertEquals("aaa", refArr.atomicGet(1), "Array: FAIL 7")
        // compareAndSet
        val aaa = "aaa"
        refArr.atomicSet(1, aaa)
        assertTrue(refArr.compareAndSet(1, aaa, "aaa1"), "Array: FAIL 8")
        assertFalse(refArr.compareAndSet(1, aaa, "aaa1"), "Array: FAIL 9")
        assertEquals("aaa1", refArr.atomicGet(1), "Array: FAIL 10")
    }

    // Tests that there are no restrictions for the array property as the receiver of the intrinsic call (it can be unknown at compile-time)
    fun testComileTimeUnknownArrayReceiver() {
        intArr.extensionArrayUpdater()
        arrayArgumentUpdater(intArr)
    }

    private fun IntArray.extensionArrayUpdater() {
        atomicSet(1, 45)
        assertTrue(compareAndSet(1, 45, 50))
        assertEquals(50, atomicGet(1))
    }

    private fun arrayArgumentUpdater(arr: IntArray) {
        arr.atomicSet(1, 45)
        assertTrue(arr.compareAndSet(1, 45, 50))
        assertEquals(50, arr.atomicGet(1))
    }
}

fun testStaticIntArrayIntrinsics() {
    // atomicGet
    val res = intArrStatic.atomicGet(2)
    assertEquals(20, res, "static IntArray: FAIL 1")
    // compareAndExchange
    val res1 = intArrStatic.compareAndExchange(2, 20, 222) // success
    assertTrue(res1 == 20 && intArrStatic[2] == 222, "static IntArray: FAIL 2")
    val res2 = intArrStatic.compareAndExchange(2, 222, 2222) // success
    assertTrue(res2 == 222 && intArrStatic[2] == 2222, "static IntArray: FAIL 3")
    val res3 = intArrStatic.compareAndExchange(2, 223, 22222) // should fail
    assertTrue(res3 == 2222 && intArrStatic[2] == 2222, "static IntArray: FAIL 4")
    val res4 = intArrStatic.compareAndExchange(9, 10, 999) // should fail
    assertTrue(res4 == 90 && intArrStatic[9] == 90, "static IntArray: FAIL 5")
    // getAndSet
    assertEquals(2222, intArrStatic.getAndSet(2, 20), "static IntArray: FAIL 6")
    assertEquals(20, intArrStatic.getAndSet(2, 200), "static IntArray: FAIL 7")
    assertEquals(200, intArrStatic.atomicGet(2), "static IntArray: FAIL 8")
    // atomicSet
    intArrStatic.atomicSet(1, 111)
    assertEquals(111, intArrStatic.atomicGet(1), "static IntArray: FAIL 9")
    // getAndAdd
    assertEquals(111, intArrStatic.getAndAdd(1, 100), "static IntArray: FAIL 10")
    assertEquals(211, intArrStatic.atomicGet(1), "static IntArray: FAIL 11")
    assertEquals(211, intArrStatic.getAndAdd(1, -100), "static IntArray: FAIL 12")
    assertEquals(111, intArrStatic.atomicGet(1), "static IntArray: FAIL 13")
    // compareAndSet
    val aaa = 1111
    intArrStatic.atomicSet(1, aaa)
    assertTrue(intArrStatic.compareAndSet(1, 1111, 2222), "static IntArray: FAIL 14")
    assertFalse(intArrStatic.compareAndSet(1, 1111, 3333), "static IntArray: FAIL 15")
    assertTrue(intArrStatic.atomicGet(1) == 2222, "static IntArray: FAIL 16")
}

fun testStaticLongArrayIntrinsics() {
// atomicGet
    val res = longArrStatic.atomicGet(2)
    assertEquals(20L, res, "IntArray: FAIL 1")
    // compareAndExchange
    val res1 = longArrStatic.compareAndExchange(2, 20L, 222L) // success
    assertTrue(res1 == 20L && longArrStatic[2] == 222L, "LongArray: FAIL 2")
    val res2 = longArrStatic.compareAndExchange(2, 222L, 2222L) // success
    assertTrue(res2 == 222L && longArrStatic[2] == 2222L, "LongArray: FAIL 3")
    val res3 = longArrStatic.compareAndExchange(2, 223L, 22222L) // should fail
    assertTrue(res3 == 2222L && longArrStatic[2] == 2222L, "LongArray: FAIL 4")
    val res4 = longArrStatic.compareAndExchange(9, 10L, 999L) // should fail
    assertTrue(res4 == 90L && longArrStatic[9] == 90L, "LongArray: FAIL 5")
    // getAndSet
    assertEquals(2222L, longArrStatic.getAndSet(2, 20L), "LongArray: FAIL 6")
    assertEquals(20L, longArrStatic.getAndSet(2, 200L), "LongArray: FAIL 7")
    assertEquals(200L, longArrStatic.atomicGet(2), "LongArray: FAIL 8")
    // atomicSet
    longArrStatic.atomicSet(1, 111L)
    assertEquals(111L, longArrStatic.atomicGet(1), "LongArray: FAIL 9")
    // getAndAdd
    assertEquals(111L, longArrStatic.getAndAdd(1, 100L), "LongArray: FAIL 10")
    assertEquals(211L, longArrStatic.atomicGet(1), "LongArray: FAIL 11")
    assertEquals(211L, longArrStatic.getAndAdd(1, -100L), "LongArray: FAIL 12")
    assertEquals(111L, longArrStatic.atomicGet(1), "LongArray: FAIL 13")
    // compareAndSet
    val aaa = 1111L
    longArrStatic.atomicSet(1, aaa)
    assertTrue(longArrStatic.compareAndSet(1, 1111L, 2222L), "LongArray: FAIL 14")
    assertFalse(longArrStatic.compareAndSet(1, 1111L, 3333L), "LongArray: FAIL 15")
    assertTrue(longArrStatic.atomicGet(1) == 2222L, "LongArray: FAIL 16")
}

fun testStaticReferenceArrayIntrinsics() {
    // atomicGet
    assertTrue(refArrStatic.atomicGet(0) == refArrStatic[0], "static Array: FAIL 1")
    // compareAndExchange
    val newValue = "aaa"
    val res1 = refArrStatic.compareAndExchange(3, null, newValue)
    assertTrue(res1 == null && refArrStatic[3] == newValue, "static Array: FAIL 2")
    val res2 = refArrStatic.compareAndExchange(3, newValue, "bbb")
    assertTrue(res2 == newValue && refArrStatic[3] == "bbb", "static Array: FAIL 3")
    val res3 = refArrStatic.compareAndExchange(3, newValue, "ccc")
    assertTrue(res3 == "bbb" && refArrStatic[3] == "bbb", "static Array: FAIL 4")
    // getAndSet
    val res4 = refArrStatic.getAndSet(4, "aaa")
    assertEquals(null, res4, "static Array: FAIL 5")
    val res5 = refArrStatic.getAndSet(4, "bbb")
    assertEquals("aaa", res5, "static Array: FAIL 6")
    // atomicSet
    refArrStatic.atomicSet(1, "aaa")
    assertEquals("aaa", refArrStatic.atomicGet(1), "static Array: FAIL 7")
    // compareAndSet
    val aaa = "aaa"
    refArrStatic.atomicSet(1, aaa)
    assertTrue(refArrStatic.compareAndSet(1, aaa, "aaa1"), "static Array: FAIL 8")
    assertFalse(refArrStatic.compareAndSet(1, aaa, "aaa1"), "static Array: FAIL 9")
    assertEquals("aaa1", refArrStatic.atomicGet(1), "static Array: FAIL 10")
}

fun box() : String {
    val testClass = ArrayIntrinsicsSmokeTest()
    testClass.testIntArrayIntrinsics()
    testClass.testLongArrayIntrinsics()
    testClass.testReferenceArrayIntrinsics()
    testClass.testComileTimeUnknownArrayReceiver()
    testStaticIntArrayIntrinsics()
    testStaticLongArrayIntrinsics()
    testStaticReferenceArrayIntrinsics()
    return "OK"
}
