// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR

import java.lang.AssertionError
import kotlin.math.abs

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC1(val x: Double) {
    fun equals(other: IC1): Boolean {
        return abs(x - other.x) < 0.5
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC2(val x: Int) {
    override fun equals(other: Any?): Boolean {
        if (other !is IC2) {
            return false
        }
        return abs(x - other.x) < 2
    }
}

fun box(): String {
    val a1Typed: IC1 = IC1(1.0)
    val b1Typed: IC1 = IC1(1.1)
    val c1Typed: IC1 = IC1(5.0)
    val a1Untyped: Any = a1Typed
    val b1Untyped: Any = b1Typed
    val c1Untyped: Any = c1Typed

    val a2Typed: IC2 = IC2(1)
    val b2Typed: IC2 = IC2(2)
    val c2Typed: IC2 = IC2(5)
    val a2Untyped: Any = a2Typed
    val b2Untyped: Any = b2Typed
    val c2Untyped: Any = c2Typed

    if ((a1Typed == b1Typed) != (a1Untyped == b1Untyped)) return "Fail 1"
    if ((a1Typed == c1Typed) != (a1Untyped == c1Untyped)) return "Fail 2"
    if ((a2Typed == b2Typed) != (a2Untyped == b2Untyped)) return "Fail 3"
    if ((a2Typed == c2Typed) != (a2Untyped == c2Untyped)) return "Fail 4"

    return "OK"
}