// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T) {
    constructor(vararg ys: Long) : this(ys.size as T)
}

fun box(): String {
//    val z1 = Z<Int>(111) OVERLOAD_RESOLUTION_AMBIGUITY
//    if (z1.x != 111) throw AssertionError()

    val z2 = Z<Int>()
    if (z2.x != 0) throw AssertionError()

    val z3 = Z<Int>(2222L)
    if (z3.x != 1) throw AssertionError()

    return "OK"
}