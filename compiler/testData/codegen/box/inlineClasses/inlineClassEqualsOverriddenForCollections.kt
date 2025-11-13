// WITH_STDLIB
// LANGUAGE: +ValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// IGNORE_HEADER_MODE: JVM_IR
//   Reason: KT-82311

import kotlin.math.abs

@JvmInline
value class IC(val x: Double) {
    operator fun equals(other: IC): Boolean {
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
