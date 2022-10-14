// IGNORE_BACKEND_FIR: JVM_IR
// https://youtrack.jetbrains.com/issue/KT-52236/Different-modality-in-psi-and-fir
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

import kotlin.math.abs

OPTIONAL_JVM_INLINE_ANNOTATION
value class MFVC(val x: Int, val y: Int) {
    override fun equals(other: Any?): Boolean {
        if (other !is MFVC) {
            return false
        }
        return abs(x - other.x) < 2 && abs(y - other.y) < 2
    }

    override fun hashCode() = 0
}

fun box(): String {
    val set = setOf(MFVC(1, 2), MFVC(2, 3), MFVC(5, 6))
    return if (set.size == 2) "OK" else "Fail"
}
