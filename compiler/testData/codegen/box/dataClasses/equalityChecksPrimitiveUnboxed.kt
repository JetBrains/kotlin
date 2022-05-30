// The purpose of this test is to ensure that we don't generate any primitive boxing in the implementation
// of a data class. See KT-48635.

data class VBoolean(val value: Boolean)

data class VByte(val value: Byte)

data class VChar(val value: Char)

data class VShort(val value: Short)

data class VInt(val value: Int)

data class VLong(val value: Long)

data class VFloat(val value: Float)

data class VDouble(val value: Double)

fun box(): String {
    if (VBoolean(true) == VBoolean(false)) return "Fail 0"
    if (VByte(0) == VByte(1)) return "Fail 1"
    if (VChar('a') == VChar('b')) return "Fail 2"
    if (VShort(0) == VShort(1)) return "Fail 3"
    if (VInt(0) == VInt(1)) return "Fail 4"
    if (VLong(0L) == VLong(1L)) return "Fail 5"
    if (VFloat(0f) == VFloat(1f)) return "Fail 6"
    if (VDouble(0.0) == VDouble(1.0)) return "Fail 7"
    return "OK"
}

// CHECK_BYTECODE_TEXT
// 0 java/lang/Boolean.valueOf
// 0 java/lang/Byte.valueOf
// 0 java/lang/Character.valueOf
// 0 java/lang/Short.valueOf
// 0 java/lang/Integer.valueOf
// 0 java/lang/Long.valueOf
// 0 java/lang/Float.valueOf
// 0 java/lang/Double.valueOf
// 1 java/lang/Float.compare
// 1 java/lang/Double.compare
