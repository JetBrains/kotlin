// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int) {
    public constructor() : this(0)
    internal constructor(x: Long, y: Int): this(x.toInt(), y.toInt())
    private constructor(x: Int, y: Int): this(x + y)
}

fun box(): String {
    val z1 = Z(111)
    if (z1.x != 111) throw AssertionError()

    val z2 = Z()
    if (z2.x != 0) throw AssertionError()

    val z3 = Z(2222L, 100)
    if (z3.x != 2322) throw AssertionError()

    return "OK"
}
