// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR, JS_IR

inline class X(val x: String)
inline class Y(val y: Number)

inline class NX(val x: String?)
inline class NY(val y: Number?)

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