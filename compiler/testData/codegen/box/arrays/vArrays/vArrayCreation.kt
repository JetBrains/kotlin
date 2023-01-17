// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses
// ENABLE_JVM_IR_INLINER


@JvmInline
value class ICDoubleNullable(val x: Double?)

@JvmInline
value class ICChar(val x: Char)

@JvmInline
value class ICICChar(val x: ICChar)

inline fun <reified T> testVArray(initVal: T): Boolean {
    val array = VArray(2) { initVal }
    return array[0] == initVal && array[1] == initVal
}

fun box(): String {

    if (!testVArray(true)) return "Fail 1"
    if (!testVArray(1.toByte())) return "Fail 2"
    if (!testVArray(2.toShort())) return "Fail 3"
    if (!testVArray(3)) return "Fail 4"
    if (!testVArray(4.toLong())) return "Fail 5"
    if (!testVArray(5.toFloat())) return "Fail 6"
    if (!testVArray(6.toDouble())) return "Fail 7"
    if (!testVArray('a')) return "Fail 8"

    if (!testVArray(1.toUByte())) return "Fail 9"
    if (!testVArray(2.toUShort())) return "Fail 10"
    if (!testVArray(3.toUInt())) return "Fail 11"
    if (!testVArray(4.toULong())) return "Fail 12"

    if (!testVArray("a")) return "Fail 13"
    if (!testVArray<ULong?>(null)) return "Fail 14"
    if (!testVArray<ICDoubleNullable>(ICDoubleNullable(42.0))) return "Fail 15"
    if (!testVArray(ICICChar(ICChar('a')))) return "Fail 16"

    return "OK"
}

// CHECK_BYTECODE_TEXT
// 1 NEWARRAY T_BOOLEAN
// 2 NEWARRAY T_BYTE
// 2 NEWARRAY T_SHORT
// 2 NEWARRAY T_INT
// 2 NEWARRAY T_LONG
// 1 NEWARRAY T_FLOAT
// 1 NEWARRAY T_DOUBLE
// 2 NEWARRAY T_CHAR
// 1 ANEWARRAY java/lang/String
// 1 ANEWARRAY kotlin/ULong
// 1 ANEWARRAY java/lang/Double
// 1 ANEWARRAY java/lang/Object