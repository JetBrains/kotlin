@file:Suppress("RESERVED_MEMBER_INSIDE_VALUE_CLASS")

// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class I1

value object O1: I1()

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class I2: I1()

value object O2: I2()

OPTIONAL_JVM_INLINE_ANNOTATION
value class I3(val value: String): I2()


fun <T> boxValue(v: T): T = v

fun <T: I1> coerceToI1(v: T): I1 = v

fun <T: I2> coerceToI2(v: T): I2 = v


fun box(): String {
    var res: Int = 0
    val i3 = I3("A")
    res = i3.hashCode()
    if (res != "A".hashCode()) return "FAIL 1: $res"

    res = boxValue(i3).hashCode()
    if (res != "A".hashCode()) return "FAIL 2: $res"

    res = coerceToI2(i3).hashCode()
    if (res != "A".hashCode()) return "FAIL 3: $res"

    res = coerceToI1(i3).hashCode()
    if (res != "A".hashCode()) return "FAIL 4: $res"

    return "OK"
}
