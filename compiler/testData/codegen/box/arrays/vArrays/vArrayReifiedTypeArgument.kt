// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER

inline fun <reified T> getFirst(p: VArray<T>) = p[0]

inline fun <reified T> setFirst(p: VArray<T>, value: T) {
    p[0] = value
}

inline fun <reified T> testVArray(initVal: T, anotherVal: T): Boolean {
    val array = VArray<T>(2) { initVal }
    if (getFirst(array) != initVal) return false
    setFirst(array, anotherVal)
    if (getFirst(array) != anotherVal) return false
    return true
}

@JvmInline
value class IcInt(val x: Int)

fun box(): String {

    if (!testVArray(false, true)) return "Fail 1"
    if (!testVArray(0.toByte(), 1.toByte())) return "Fail 2"
    if (!testVArray(0.toShort(), 1.toShort())) return "Fail 3"
    if (!testVArray(0, 1)) return "Fail 4"
    if (!testVArray(0.toLong(), 0.toLong())) return "Fail 5"
    if (!testVArray(0.toFloat(), 1.toFloat())) return "Fail 6"
    if (!testVArray(0.toDouble(), 1.toDouble())) return "Fail 7"
    if (!testVArray('a', 'b')) return "Fail 8"

    if (!testVArray(0.toUByte(), 1.toUByte())) return "Fail 9"
    if (!testVArray(0.toUShort(), 1.toUShort())) return "Fail 10"
    if (!testVArray(0.toUInt(), 1.toUInt())) return "Fail 11"
    if (!testVArray(0.toULong(), 0.toULong())) return "Fail 12"

    if (!testVArray("a", "b")) return "Fail 13"
    if (!testVArray(IcInt(0), IcInt(1))) return "Fail 14"
    if (!testVArray(0, IcInt(1))) return "Fail 15"
    if (!testVArray(0, "a")) return "Fail 16"

    return "OK"
}

// CHECK_BYTECODE_TEXT
// 1 NEWARRAY T_BOOLEAN
// 2 NEWARRAY T_BYTE
// 2 NEWARRAY T_SHORT
// 3 NEWARRAY T_INT
// 2 NEWARRAY T_LONG
// 1 NEWARRAY T_FLOAT
// 1 NEWARRAY T_DOUBLE
// 1 NEWARRAY T_CHAR
// 1 ANEWARRAY java/lang/String
// 3 ANEWARRAY java/lang/Object