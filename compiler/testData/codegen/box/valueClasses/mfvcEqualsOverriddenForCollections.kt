// WITH_STDLIB
// LANGUAGE: +ValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

import kotlin.math.abs

@JvmInline
@AllowTypedEquals
value class MFVC(val x: Double, val y: Int) {
    @TypedEquals
    fun equals(other: MFVC): Boolean {
        return abs(x - other.x) < 0.1
    }

    override fun hashCode(): Int {
        return 0
    }
}

@OptIn(AllowTypedEquals::class)
fun box(): String {
    val set = setOf(MFVC(1.0, 100), MFVC(1.5, 200), MFVC(1.501, 300))
    return if (set.size == 2) "OK" else "Fail"
}
