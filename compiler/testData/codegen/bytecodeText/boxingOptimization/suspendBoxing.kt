inline fun fInt(g: (Int) -> Unit) {
    g(1)
}

inline fun fBoolean(g: (Boolean) -> Unit) {
    g(true)
}

inline fun fChar(g: (Char) -> Unit) {
    g('a')
}

inline fun fByte(g: (Byte) -> Unit) {
    g(1)
}

inline fun fShort(g: (Short) -> Unit) {
    g(1)
}

inline fun fFloat(g: (Float) -> Unit) {
    g(1.0f)
}

inline fun fLong(g: (Long) -> Unit) {
    g(1L)
}

inline fun fDouble(g: (Double) -> Unit) {
    g(1.0)
}

fun bar() {
    fInt { }
    fBoolean { }
    fChar { }
    fByte { }
    fShort { }
    fFloat { }
    fLong { }
    fDouble { }
}

suspend fun baz() {
    fInt { }
    fBoolean { }
    fChar { }
    fByte { }
    fShort { }
    fFloat { }
    fLong { }
    fDouble { }
}

// The inline functions will contain boxing for the value passed to the lambda.
// 8 valueOf

// After inlining there will be boxing and unboxing that is not needed. That should be optimized out.
// 0 intValue
// 0 booleanValue
// 0 charValue
// 0 byteValue
// 0 shortValue
// 0 floatValue
// 0 longValue
// 0 doubleValue
