// ENABLE_JVM_IR_INLINER
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

fun testBoolVarray(): Boolean {
    val arr = VArray(3) { it > 0 }
    return stringifyByIteration(arr) == "falsetruetrue"
}

fun testCharVArray(): Boolean {
    val arr = VArray(3) { (it + 'a'.code).toChar() }
    return stringifyByIteration(arr) == "abc"
}

fun testByteVArray(): Boolean {
    val arr = VArray(3) { it.toByte() }
    return stringifyByIteration(arr) == "012"
}

fun testShortVArray(): Boolean {
    val arr = VArray(3) { it.toShort() }
    return stringifyByIteration(arr) == "012"
}

fun testIntVArray(): Boolean {
    val arr = VArray(3) { it }
    return stringifyByIteration(arr) == "012"
}

fun testLongVArray(): Boolean {
    val arr = VArray(3) { it }
    return stringifyByIteration(arr) == "012"
}

fun testFloatVArray(): Boolean {
    val arr = VArray(3) { it.toFloat() }
    return stringifyByIteration(arr) == "0.01.02.0"
}

fun testDoubleVArray(): Boolean {
    val arr = VArray(3) { it.toFloat() }
    return stringifyByIteration(arr) == "0.01.02.0"
}

fun testStringVArray(): Boolean {
    val arr = VArray(3) { it.toString() }
    return stringifyByIteration(arr) == "012"
}

inline fun <reified T> stringifyByIteration(arr: VArray<T>): String {
    val stringBuilder = StringBuilder()
    val iterator = arr.iterator()
    while (iterator.hasNext()) {
        stringBuilder.append(iterator.next())
    }
    return stringBuilder.toString()
}

@JvmInline
value class IC(val x: Int)

fun testICVarray(): Boolean {
    val arr = VArray(3) { IC(it) }
    return stringifyByIteration(arr) == "IC(x=0)IC(x=1)IC(x=2)"
}

fun box(): String {
    if (!testBoolVarray()) return "Fail 1"
    if (!testCharVArray()) return "Fail 2"
    if (!testByteVArray()) return "Fail 3"
    if (!testShortVArray()) return "Fail 4"
    if (!testIntVArray()) return "Fail 5"
    if (!testLongVArray()) return "Fail 6"
    if (!testFloatVArray()) return "Fail 7"
    if (!testDoubleVArray()) return "Fail 8"
    if (!testStringVArray()) return "Fail 9"
    if (!testICVarray()) return "Fail 10"

    return "OK"
}