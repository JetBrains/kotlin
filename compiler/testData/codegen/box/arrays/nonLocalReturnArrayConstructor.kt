typealias ArrayS = Array<String>

fun testArray() {
    Array<String>(5) { i ->
        if (i == 3) return
        i.toString()
    }
    throw AssertionError()
}

fun testArrayAlias() {
    ArrayS(5) { i ->
        if (i == 3) return
        i.toString()
    }
    throw AssertionError()
}

fun testIntArray() {
    IntArray(5) { i ->
        if (i == 3) return
        i
    }
    throw AssertionError()
}

fun testLongArray() {
    LongArray(5) { i ->
        if (i == 3) return
        i.toLong()
    }
    throw AssertionError()
}

fun testBooleanArray() {
    BooleanArray(5) { i ->
        if (i == 3) return
        i % 2 == 0
    }
    throw AssertionError()
}

fun testCharArray() {
    CharArray(5) { i ->
        if (i == 3) return
        i.toChar()
    }
    throw AssertionError()
}

fun testFloatArray() {
    FloatArray(5) { i ->
        if (i == 3) return
        i.toFloat()
    }
    throw AssertionError()
}

fun testDoubleArray() {
    DoubleArray(5) { i ->
        if (i == 3) return
        i.toDouble()
    }
    throw AssertionError()
}

fun box(): String {
    testArray()
    testArrayAlias()
    testIntArray()
    testLongArray()
    testBooleanArray()
    testCharArray()
    testFloatArray()
    testDoubleArray()
    return "OK"
}
