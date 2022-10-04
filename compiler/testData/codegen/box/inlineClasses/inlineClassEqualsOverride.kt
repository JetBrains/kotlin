// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR

import kotlin.math.abs

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC1(val value: Double) {
    fun equals(other: IC1): Boolean {
        return abs(value - other.value) < 0.1
    }
}

interface I {
    fun equals(param: IC2): Boolean
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC2(val value: Int) : I {
    override fun equals(param: IC2): Boolean {
        return abs(value - param.value) < 2
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC3(val value: Int) {

}

fun box() =
    if (IC1(1.0) == IC1(1.05) && IC1(1.0) != IC1(1.2)
        && IC2(5) == IC2(6) && IC2(5) != IC2(7)
        && IC3(5) == IC3(5) && IC3(5) != IC3(6)
        && IC1(1.0) != Any()) "OK" else "Fail"