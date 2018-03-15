package runtime.collections.typed_array1

import kotlin.test.*

@Test fun runTest() {
    val array = ByteArray(17)
    val results = mutableSetOf<Any>()
    var counter = 0
    try {
        results += array.shortAt(16)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        results += array.charAt(22)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        results += array.intAt(15)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        results += array.longAt(14)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        results += array.floatAt(14)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        results += array.doubleAt(13)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }

    try {
        array.setShortAt(16, 2.toShort())
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        array.setCharAt(22, 'a')
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        array.setIntAt(15, 1234)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        array.setLongAt(14, 1.toLong())
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        array.setFloatAt(14, 1.0f)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }
    try {
        array.setDoubleAt(13, 3.0)
    } catch (e: ArrayIndexOutOfBoundsException) {
        counter++
    }

    expect(12) { counter }
    expect(0) { results.size }
    println("OK")
}