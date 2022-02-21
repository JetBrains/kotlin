// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: JS_IR
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class X<T: String>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Y<T: Number>(val y: T)


OPTIONAL_JVM_INLINE_ANNOTATION
value class NX<T: String?>(val x: T)


OPTIONAL_JVM_INLINE_ANNOTATION
value class NX2<T: String>(val x: T?)

OPTIONAL_JVM_INLINE_ANNOTATION
value class NY<T: Number?>(val y: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class NY2<T: Number>(val y: T?)

fun testNotNull(x: X<String>?, y: Y<Number>?) {
    val xs = listOf<Any?>(x)
    val ys = listOf<Any?>(y)
    if (!xs.contains(y)) throw AssertionError()
    if (xs[0] != ys[0]) throw AssertionError()
    if (xs[0] !== ys[0]) throw AssertionError()
}

fun testNullable(x: NX<String?>?, y: NY<Number?>?) {
    val xs = listOf<Any?>(x)
    val ys = listOf<Any?>(y)
    if (xs.contains(y)) throw AssertionError()
    if (xs[0] == ys[0]) throw AssertionError()
    if (xs[0] === ys[0]) throw AssertionError()
}

fun testNullable2(x: NX2<String>?, y: NY2<Number>?) {
    val xs = listOf<Any?>(x)
    val ys = listOf<Any?>(y)
    if (xs.contains(y)) throw AssertionError()
    if (xs[0] == ys[0]) throw AssertionError()
    if (xs[0] === ys[0]) throw AssertionError()
}

fun testNullsAsNullable(x: NX<String?>?, y: NY<Number?>?) {
    val xs = listOf<Any?>(x)
    val ys = listOf<Any?>(y)
    if (!xs.contains(y)) throw AssertionError()
    if (xs[0] != ys[0]) throw AssertionError()
    if (xs[0] !== ys[0]) throw AssertionError()
}

fun testNullsAsNullable2(x: NX2<String>?, y: NY2<Number>?) {
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

    testNullable2(NX2(null), NY2(null))
    testNullable2(NX2(null), null)
    testNullable2(null, NY2(null))

    testNullsAsNullable(null, null)

    testNullsAsNullable2(null, null)

    return "OK"
}