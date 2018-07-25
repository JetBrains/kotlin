// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS
// WITH_RUNTIME

fun checkByteArray(): Boolean {
    val byteArray = byteArrayOf(1, 2, 3)
    var sum = 0
    for (i in 0..byteArray.size - 1) {
        sum += byteArray[i]
    }
    if (sum != 6) return false
    return true
}

fun checkShortArray(): Boolean {
    val shortArray = shortArrayOf(1, 2, 3)
    var sum = 0
    for (i in 0..shortArray.size - 1) {
        sum += shortArray[i]
    }
    if (sum != 6) return false
    return true
}

fun checkCharArray(): Boolean {
    val charArray = charArrayOf('1', '2', '3')
    var sum = ""
    for (i in 0..charArray.size - 1) {
        sum += charArray[i]
    }
    if (sum != "123") return false
    return true
}

fun checkIntArray(): Boolean {
    val intArray = intArrayOf(1, 2, 3)
    var sum = 0
    for (i in 0..intArray.size - 1) {
        sum += intArray[i]
    }
    if (sum != 6) return false
    return true
}

fun checkLongArray(): Boolean {
    val longArray = longArrayOf(1L, 2L, 3L)
    var sum = 0L
    for (i in 0..longArray.size - 1) {
        sum += longArray[i]
    }
    if (sum != 6L) return false
    return true
}

fun checkFloatArray(): Boolean {
    val floatArray = floatArrayOf(1.1f, 2.2f, 3.3f)
    var sum = 0f
    for (i in 0..floatArray.size - 1) {
        sum += floatArray[i]
    }
    if (sum != (1.1f + 2.2f + 3.3f)) return false
    return true
}

fun checkDoubleArray(): Boolean {
    val doubleArray = doubleArrayOf(1.1, 2.2, 3.3)
    var sum = 0.0
    for (i in 0..doubleArray.size - 1) {
        sum += doubleArray[i]
    }
    if (sum != 6.6) return false
    return true
}

fun checkBooleanArray(): Boolean {
    val booleanArray = booleanArrayOf(false, false, true)
    var result = false
    for (i in 0..booleanArray.size - 1) {
        result = booleanArray[i]
    }
    return result
}

class Value(val value: Int) {}

fun checkObjectArray(): Boolean {
    val objectArray = arrayOf(Value(1), Value(2), Value(3))
    var sum = 0
    for (i in 0..objectArray.size - 1) {
        sum += objectArray[i].value
    }
    if (sum != 6) return false
    return true
}

fun checkWithArrayUpdate(): Boolean {
    var intArray = intArrayOf(1, 2, 3)
    var sum = 0
    for (i in 0..intArray.size - 1) {
        sum += intArray[i]
        intArray = intArrayOf(4, 5, 6, 7)
    }
    if (sum != 12) return false
    return true
}

fun checkIntArrayMinusArbitraryConstant(): Boolean {
    val intArray = intArrayOf(1, 2, 3)
    var sum = 0
    for (i in 0..intArray.size - 2) {
        sum += intArray[i]
    }
    if (sum != 3) return false
    return true
}

fun checkReversedIntArray(): Boolean {
    val intArray = intArrayOf(1, 2, 3)
    var start = 0
    var sum = 0
    for (i in (start..intArray.size - 1).reversed()) {
        sum += intArray[i]
    }
    if (sum != 6) return false
    return true
}

fun checkIntArrayMethodCallBound(): Boolean {
    val intArray = intArrayOf(1, 2, 3)
    var start = 0
    var sum = 0
    for (i in 0..kotlin.math.min(intArray.size, 10) - 1) {
        sum += intArray[i]
    }
    if (sum != 6) return false
    return true
}

fun box(): String {
    // Check that the specialization of 'for (i in 0..array.size-1)' to 'for (i in 0 until array.size)' does not fail on
    // any kind of arrays.
    if (!checkByteArray()) return "Failure"
    if (!checkShortArray()) return "Failure"
    if (!checkCharArray()) return "Failure"
    if (!checkIntArray()) return "Failure"
    if (!checkLongArray()) return "Failure"
    if (!checkFloatArray()) return "Failure"
    if (!checkDoubleArray()) return "Failure"
    if (!checkBooleanArray()) return "Failure"
    if (!checkObjectArray()) return "Failure"
    if (!checkWithArrayUpdate()) return "Failure"
    if (!checkIntArrayMinusArbitraryConstant()) return "Failure"
    if (!checkReversedIntArray()) return "Failure"
    if (!checkIntArrayMethodCallBound()) return "Failure"

    return "OK"
}