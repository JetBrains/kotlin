// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

// The purpose of this test is to ensure that we don't generate any primitive boxing in the implementation
// of a @JvmInline value class. See KT-48635.

OPTIONAL_JVM_INLINE_ANNOTATION
value class VBoolean(val value: Boolean)

OPTIONAL_JVM_INLINE_ANNOTATION
value class VByte(val value: Byte)

OPTIONAL_JVM_INLINE_ANNOTATION
value class VChar(val value: Char)

OPTIONAL_JVM_INLINE_ANNOTATION
value class VShort(val value: Short)

OPTIONAL_JVM_INLINE_ANNOTATION
value class VInt(val value: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class VLong(val value: Long)

OPTIONAL_JVM_INLINE_ANNOTATION
value class VFloat(val value: Float)

OPTIONAL_JVM_INLINE_ANNOTATION
value class VDouble(val value: Double)

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
// 2 java/lang/Float.compare
// 2 java/lang/Double.compare
