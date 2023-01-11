// ENABLE_JVM_IR_INLINER
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

inline fun <reified T> stringifyByIterationExplicitToString(arr: VArray<T>): String {
    val stringBuilder = StringBuilder()
    val iterator = arr.iterator()
    while (iterator.hasNext()) {
        stringBuilder.append(iterator.next().toString())
    }
    return stringBuilder.toString()
}

inline fun <reified T> stringifyByIterationImplicitToString(arr: VArray<T>): String {
    val stringBuilder = StringBuilder()
    val iterator = arr.iterator()
    while (iterator.hasNext()) {
        stringBuilder.append(iterator.next())
    }
    return stringBuilder.toString()
}

inline fun <reified T> testArray(arr: VArray<T>, expected: String) =
    stringifyByIterationExplicitToString(arr) == expected && stringifyByIterationImplicitToString((arr)) == expected

@JvmInline
value class IcInt(val x: Int)

@JvmInline
value class IcIcInt(val x: IcInt)

@JvmInline
value class IcStr(val x: String)

@JvmInline
value class IcIntN(val x: Int?)

@JvmInline
value class IcIcIntN(val x : IcInt?)

fun testBoolVarray() = testArray(VArray(3) { it > 0 }, "falsetruetrue")

fun testCharVArray() = testArray(VArray(3) { (it + 'a'.code).toChar() }, "abc")

fun testByteVArray() = testArray(VArray(3) { it.toByte() }, "012")

fun testShortVArray() = testArray(VArray(3) { it.toShort() }, "012")

fun testIntVArray() = testArray(VArray(3) { it }, "012")

fun testLongVArray() = testArray(VArray(3) { it }, "012")

fun testFloatVArray() = testArray(VArray(3) { it.toFloat() }, "0.01.02.0")

fun testDoubleVArray() = testArray(VArray(3) { it.toFloat() }, "0.01.02.0")

fun testStringVArray() = testArray(VArray(3) { it.toString() }, "012")

fun testIntNVArray() = testArray(VArray(3){if (it > 0) it else null}, "null12")
fun testStringNVArray() = testArray(VArray(3){if (it > 0) it.toString() else null}, "null12")

fun testIcIntVarray() = testArray(VArray(3) { IcInt(it) }, "IcInt(x=0)IcInt(x=1)IcInt(x=2)")

fun testIcIcIntVArray() = testArray(VArray(3) { IcIcInt(IcInt(it)) }, "IcIcInt(x=IcInt(x=0))IcIcInt(x=IcInt(x=1))IcIcInt(x=IcInt(x=2))")

fun testIcStrVArray() = testArray(VArray(3) { IcStr(it.toString()) }, "IcStr(x=0)IcStr(x=1)IcStr(x=2)")

fun testIcIntNVArray() = testArray(VArray(3) { IcIntN(if (it > 0) it else null) }, "IcIntN(x=null)IcIntN(x=1)IcIntN(x=2)")

fun testIcIcIntNVArray() = testArray(VArray(3){IcIcIntN(if (it > 0) IcInt(it) else null)}, "IcIcIntN(x=null)IcIcIntN(x=IcInt(x=1))IcIcIntN(x=IcInt(x=2))")

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

    if (!testIntNVArray()) return "Fail 10"
    if (!testStringNVArray()) return "Fail 11"

    if (!testIcIntVarray()) return "Fail 12"
    if (!testIcIcIntVArray()) return "Fail 13"
    if (!testIcStrVArray()) return "Fail 14"
    if (!testIcIntNVArray()) return "Fail 15"
    if (!testIcIcIntNVArray()) return "Fail 16"

    return "OK"
}