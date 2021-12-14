// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int) {
    constructor(x: Long = 42L) : this(x.toInt())
}

fun box(): String {
    if (Z().x != 42) throw AssertionError()

    return "OK"
}