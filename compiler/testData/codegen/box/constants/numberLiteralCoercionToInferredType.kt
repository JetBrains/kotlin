// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// FIR status: value: 0 should have type Long
// WITH_STDLIB

// FILE: J.java

public class J {
    public static long platformLong() {
        return 42;
    }

    public static Comparable<Long> platformCLong() {
        return new Long(42);
    }
}

// FILE: test.kt

inline fun <reified T> check(value: Any?) {
    if (value !is T) throw Exception("value: $value should have type ${T::class.simpleName}")
}

fun <K> selectFirst(vararg xs: K): K = xs[0]

fun takeNLong(nL: Long?) {}

fun <T> checkArray(array: T, copy: T.() -> T, toList: T.() -> List<*>, check: (T, T) -> Boolean, modify: T.() -> Unit) {}

fun testFromStdlib() {
    checkArray(arrayOf("a", 1, null), { copyOf() }, { toList() }, { a1, a2 -> a1 contentEquals a2 }, { reverse() })
}

fun box(): String {
    check<Long>(selectFirst(0, 0L))
    check<Byte>(selectFirst(0, 0.toByte()))
    check<Short>(selectFirst(0, 0.toShort()))

    takeNLong(0)

    val cLong: Comparable<Long> = 0L
    check<Long>(selectFirst(0, cLong))

    val cByte: Comparable<Byte> = 0.toByte()
    check<Byte>(selectFirst(0, cByte))

    val cShort: Comparable<Short> = 0.toShort()
    check<Short>(selectFirst(0, cShort))

    val cStar: Comparable<*> = 0L
    check<Int>(selectFirst(0, cStar))

    check<Long>(selectFirst(0, J.platformLong()))
    check<Long>(selectFirst(0, J.platformCLong()))

    check<Int>(selectFirst(0, 0L, "string"))
    check<Int>(selectFirst(0, 0L, true))
    check<Int>(selectFirst(0, 0L, 0.toByte()))
    check<Int>(selectFirst(0, 0L, 0f))
    check<Int>(selectFirst(0, 0L, 0f, 0.0))

    val r = 0
    check<Int>(
        when (r) {
            0 -> 0
            1 -> 0L
            2 -> "string"
            else -> TODO()
        }
    )

    check<Int>(selectFirst(0, 0L, 0.0, null))

    check<ULong>(selectFirst(0u, 0uL))
    check<UByte>(selectFirst(0u, 0.toUByte()))
    check<UShort>(selectFirst(0u, 0.toUShort()))

    check<UInt>(selectFirst(0u, 0uL, "foo"))
    check<UInt>(selectFirst(0u, 0uL, "foo", null))

    return "OK"
}