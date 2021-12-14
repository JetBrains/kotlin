// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: String)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Y(val y: Number)


OPTIONAL_JVM_INLINE_ANNOTATION
value class NX(val x: String?)

OPTIONAL_JVM_INLINE_ANNOTATION
value class NY(val y: Number?)

fun testNotNull(x: X?, y: Y?) {
    val xs = listOf<Any?>(x)
    val ys = listOf<Any?>(y)
    if (!xs.contains(y)) throw AssertionError()
    if (xs[0] != ys[0]) throw AssertionError()
    if (xs[0] !== ys[0]) throw AssertionError()
}

fun testNullable(x: NX?, y: NY?) {
    val xs = listOf<Any?>(x)
    val ys = listOf<Any?>(y)
    if (xs.contains(y)) throw AssertionError()
    if (xs[0] == ys[0]) throw AssertionError()
    if (xs[0] === ys[0]) throw AssertionError()
}

fun testNullsAsNullable(x: NX?, y: NY?) {
    val xs = listOf<Any?>(x)
    val ys = listOf<Any?>(y)
    if (!xs.contains(y)) throw AssertionError()
    if (xs[0] != ys[0]) throw AssertionError()
    if (xs[0] !== ys[0]) throw AssertionError()
}


fun box(): String {
    testNotNull(null, null)

    testNullable(NX(null), NY(null))
    testNullable(NX(null), null)
    testNullable(null, NY(null))

    testNullsAsNullable(null, null)

    return "OK"
}