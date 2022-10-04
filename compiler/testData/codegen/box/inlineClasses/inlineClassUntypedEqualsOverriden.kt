// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR

import kotlin.math.abs

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val x: Int) {
    override fun equals(other: Any?): Boolean {
        if (other !is IC) {
            return false
        }
        return abs(x - other.x) < 2
    }

    override fun hashCode() = 0
}

fun box(): String {
    val set = setOf(IC(1), IC(2), IC(5))
    return if (set.size == 2 && IC(1) == IC(1) && IC(1) == IC(2) && IC(1) != IC(5)) "OK" else "Fail"
}