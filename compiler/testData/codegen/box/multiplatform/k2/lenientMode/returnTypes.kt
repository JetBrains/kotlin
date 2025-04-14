// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// LENIENT_MODE
// IGNORE_HMPP: JVM_IR

// MODULE: common
// FILE: common.kt
package pkg

expect fun getString(): String
expect fun getBoolean(): Boolean
expect fun getByte(): Byte
expect fun getShort(): Short
expect fun getInt(): Int
expect fun getLong(): Long
expect fun getChar(): Char
expect fun getFloat(): Float
expect fun getDouble(): Double
expect fun getNull(): Any?
expect fun throwException(): Any

// MODULE: jvm()()(common)
// FILE: jvm.kt
package pkg

fun box(): String {
    if (getString() != "") return "FAIL: getString()"
    if (getBoolean() != false) return "FAIL: getBoolean()"
    if (getByte() != 0.toByte()) return "FAIL: getByte()"
    if (getShort() != 0.toShort()) return "FAIL: getShort()"
    if (getInt() != 0) return "FAIL: getInt()"
    if (getLong() != 0L) return "FAIL: getLong()"
    if (getChar() != Char.MIN_VALUE) return "FAIL: getChar()"
    if (getFloat() != 0.0f) return "FAIL: getFloat()"
    if (getDouble() != 0.0) return "FAIL: getDouble()"
    if (getNull() != null) return "FAIL: getNull()"

    try {
        throwException()
        return "FAIL: throwException()"
    } catch (e: NotImplementedError) {
        // expected
    }

    return "OK"
}
