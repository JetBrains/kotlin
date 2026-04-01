// WITH_STDLIB
// LANGUAGE: +JvmInlineMultiFieldValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

@JvmInline
value class MFVC1<T : Number>(val x: T, val other: Int) {
    fun equals(x: Int, other: Int) = false
    fun equals(other: MFVC1<*>) = true
}

class Generic<T, R>(val x: T, val y: R)

@JvmInline
value class MFVC2<T, R>(val value: Generic<T, R>, val other: Int) {
    fun equals(value: MFVC1<Double>, other: Int) = false
    fun equals(other: MFVC2<*, *>) = true
}

@JvmInline
value class MFVC3<T>(val value: T, val other: Int) {
    fun equals(value: Int, other: Int) = false
    fun equals(other: MFVC3<*>) = true
}

@JvmInline
value class MFVC4<T>(val value: T, val other: Int) {
    fun equals(value: Any, other: Int) = false
    fun equals(other: MFVC4<*>) = true
}


fun box() = when {
    MFVC1(5.0, 100) != MFVC1(3, 100) -> "Fail 1.1"
    (MFVC1(5.0, 100) as Any) != MFVC1(3, 100) -> "Fail 1.2"
    MFVC1(5.0, 100) != (MFVC1(3, 100) as Any) -> "Fail 1.3"
    (MFVC1(5.0, 100) as Any) != (MFVC1(3, 100) as Any) -> "Fail 1.4"

    MFVC2(Generic("aba", 5.0), 100) != MFVC2(Generic(3, 8), 100) -> "Fail 2.1"
    (MFVC2(Generic("aba", 5.0), 100) as Any) != MFVC2(Generic(3, 8), 100) -> "Fail 2.2"
    MFVC2(Generic("aba", 5.0), 100) != (MFVC2(Generic(3, 8), 100) as Any) -> "Fail 2.3"
    (MFVC2(Generic("aba", 5.0), 100) as Any) != (MFVC2(Generic(3, 8), 100) as Any) -> "Fail 2.4"

    MFVC3("x", 100) != MFVC3("y", 100) -> "Fail 3.1"
    (MFVC3("x", 100) as Any) != MFVC3("y", 100) -> "Fail 3.2"
    MFVC3("x", 100) != (MFVC3("y", 100) as Any) -> "Fail 3.3"
    (MFVC3("x", 100) as Any) != (MFVC3("y", 100) as Any) -> "Fail 3.4"

    MFVC4("aba", 100) != MFVC4("caba", 100) -> "Fail 4.1"
    (MFVC4("aba", 100) as Any) != MFVC4("caba", 100) -> "Fail 4.2"
    MFVC4("aba", 100) != (MFVC4("caba", 100) as Any) -> "Fail 4.3"
    (MFVC4("aba", 100) as Any) != (MFVC4("caba", 100) as Any) -> "Fail 4.4"

    else -> "OK"
}
