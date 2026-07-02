// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

OPTIONAL_JVM_INLINE_ANNOTATION
value class IntValue(val value: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Wrapper<T>(val value: T)

fun boom(w1: Wrapper<IntValue>): Boolean = w1.value == IntValue(1)
fun boomNeq(w1: Wrapper<IntValue>): Boolean = w1.value != IntValue(1)
fun boomExplicit(w1: Wrapper<IntValue>): Boolean = w1.value.equals(IntValue(1))

fun box(): String {
    val w = Wrapper(IntValue(1))
    val wOther = Wrapper(IntValue(2))

    if (!boom(w)) return "Fail 1"
    if (boom(wOther)) return "Fail 2"
    if (boomNeq(w)) return "Fail 3"
    if (!boomNeq(wOther)) return "Fail 4"
    if (!boomExplicit(w)) return "Fail 5"
    if (boomExplicit(wOther)) return "Fail 6"

    return "OK"
}
