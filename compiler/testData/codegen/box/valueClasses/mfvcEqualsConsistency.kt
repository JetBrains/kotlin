// WITH_STDLIB
// LANGUAGE: +ValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

import java.lang.AssertionError
import kotlin.math.abs

@JvmInline
value class MFVC1(val x: Int, val y: Int) {
    @TypedEquals
    fun equals(other: MFVC1): Boolean {
        return abs(x - other.x) < 2 && abs(y - other.y) < 2
    }
}


fun box(): String {
    val a1Typed: MFVC1 = MFVC1(1, 2)
    val b1Typed: MFVC1 = MFVC1(2, 3)
    val a1Untyped: Any = a1Typed
    val b1Untyped: Any = b1Typed

    require(a1Untyped == b1Untyped)


    return "OK"
}
