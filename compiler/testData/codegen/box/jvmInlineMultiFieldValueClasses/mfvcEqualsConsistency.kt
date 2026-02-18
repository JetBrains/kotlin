// WITH_STDLIB
// LANGUAGE: +JvmInlineMultiFieldValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

import java.lang.AssertionError
import kotlin.math.abs

@JvmInline
value class MFVC1(val x: Int, val y: Int) {
    fun equals(other: MFVC1): Boolean {
        return abs(x - other.x) < 2 && abs(y - other.y) < 2
    }
}

@JvmInline
value class MFVC2(val x: Int, val y: Int) {
    override fun equals(other: Any?): Boolean {
        if (other !is MFVC2) {
            return false
        }
        return abs(x - other.x) < 2 && abs(y - other.y) < 2
    }
}

fun box(): String {
    val a1Typed: MFVC1 = MFVC1(1, 2)
    val b1Typed: MFVC1 = MFVC1(2, 3)
    val c1Typed: MFVC1 = MFVC1(3, 4)
    val a1Untyped: Any = a1Typed
    val b1Untyped: Any = b1Typed
    val c1Untyped: Any = c1Typed

    val a2Typed: MFVC2 = MFVC2(1, 2)
    val b2Typed: MFVC2 = MFVC2(2, 3)
    val c2Typed: MFVC2 = MFVC2(3, 4)
    val a2Untyped: Any = a2Typed
    val b2Untyped: Any = b2Typed
    val c2Untyped: Any = c2Typed

    require(a1Typed == a1Typed && a1Untyped == a1Untyped)
    require(a1Typed == b1Typed && a1Untyped == b1Untyped)
    require(a1Typed != c1Typed && a1Untyped != c1Untyped)
    require(b1Typed == a1Typed && b1Untyped == a1Untyped)
    require(b1Typed == b1Typed && b1Untyped == b1Untyped)
    require(b1Typed == c1Typed && b1Untyped == c1Untyped)
    require(c1Typed != a1Typed && c1Untyped != a1Untyped)
    require(c1Typed == b1Typed && c1Untyped == b1Untyped)
    require(c1Typed == c1Typed && c1Untyped == c1Untyped)

    require(a2Typed == a2Typed && a2Untyped == a2Untyped)
    require(a2Typed == b2Typed && a2Untyped == b2Untyped)
    require(a2Typed != c2Typed && a2Untyped != c2Untyped)
    require(b2Typed == a2Typed && b2Untyped == a2Untyped)
    require(b2Typed == b2Typed && b2Untyped == b2Untyped)
    require(b2Typed == c2Typed && b2Untyped == c2Untyped)
    require(c2Typed != a2Typed && c2Untyped != a2Untyped)
    require(c2Typed == b2Typed && c2Untyped == b2Untyped)
    require(c2Typed == c2Typed && c2Untyped == c2Untyped)

    return "OK"
}
