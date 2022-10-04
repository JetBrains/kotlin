// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR

import kotlin.math.abs

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val x: Double) {
    fun equals(other: IC): Boolean {
        return abs(x - other.x) < 0.1
    }

    override fun hashCode(): Int {
        return 0
    }
}

fun box(): String {
    val set = setOf(IC(1.0), IC(1.5), IC(1.501))
    return if (set.size == 2) "OK" else "Fail"
}