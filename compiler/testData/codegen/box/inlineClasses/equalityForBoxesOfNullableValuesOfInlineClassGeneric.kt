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
value class NY<T: Number?>(val y: T)

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

fun testNullsAsNullable(x: NX<String?>?, y: NY<Number?>?) {
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