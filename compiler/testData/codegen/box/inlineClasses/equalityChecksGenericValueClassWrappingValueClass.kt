// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// ISSUE: KT-87327

OPTIONAL_JVM_INLINE_ANNOTATION
value class IntValue(val value: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Wrapper<out T>(val value: T)

// KT-87327: generic-unwrapped value class vs direct value-class instance
fun boom(w1: Wrapper<IntValue>): Boolean = w1.value == IntValue(1)

// Flipped order
fun flipped(w1: Wrapper<IntValue>): Boolean = IntValue(1) == w1.value

// Both sides are generic-extracted
fun bothGeneric(w1: Wrapper<IntValue>, w2: Wrapper<IntValue>): Boolean = w1.value == w2.value

// Extracted to local variable
fun viaLocal(w1: Wrapper<IntValue>): Boolean {
    val v = w1.value
    return v == IntValue(1)
}

// Wrapper-level equality
fun wrapperEquals(w1: Wrapper<IntValue>, w2: Wrapper<IntValue>): Boolean = w1 == w2

// Explicit .equals() call
fun explicitEquals(w1: Wrapper<IntValue>): Boolean = w1.value.equals(IntValue(1))

fun box(): String {
    if (!boom(Wrapper(IntValue(1)))) return "Fail 1"
    if (boom(Wrapper(IntValue(2)))) return "Fail 2"
    if (!flipped(Wrapper(IntValue(1)))) return "Fail 3"
    if (flipped(Wrapper(IntValue(2)))) return "Fail 4"
    if (!bothGeneric(Wrapper(IntValue(1)), Wrapper(IntValue(1)))) return "Fail 5"
    if (bothGeneric(Wrapper(IntValue(1)), Wrapper(IntValue(2)))) return "Fail 6"
    if (!viaLocal(Wrapper(IntValue(1)))) return "Fail 7"
    if (viaLocal(Wrapper(IntValue(2)))) return "Fail 8"
    if (!wrapperEquals(Wrapper(IntValue(1)), Wrapper(IntValue(1)))) return "Fail 9"
    if (wrapperEquals(Wrapper(IntValue(1)), Wrapper(IntValue(2)))) return "Fail 10"
    if (!explicitEquals(Wrapper(IntValue(1)))) return "Fail 11"
    if (explicitEquals(Wrapper(IntValue(2)))) return "Fail 12"
    return "OK"
}
