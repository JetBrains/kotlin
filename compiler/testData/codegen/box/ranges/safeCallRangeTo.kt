// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

fun charRange(x: Char?, y: Char) = x?.rangeTo(y)
fun byteRange(x: Byte?, y: Byte) = x?.rangeTo(y)
fun shortRange(x: Short?, y: Short) = x?.rangeTo(y)
fun intRange(x: Int?, y: Int) = x?.rangeTo(y)
fun longRange(x: Long?, y: Long) = x?.rangeTo(y)
fun floatRange(x: Float?, y: Float) = x?.rangeTo(y)
fun dougleRange(x: Double?, y: Double) = x?.rangeTo(y)

inline fun <reified T, R> testSafeRange(x: T, y: T, expectStr: String, safeRange: (T?, T) -> R?) {
    val rNull = safeRange(null, y)
    assert (rNull == null) { "${T::class.simpleName}: Expected: null, got $rNull" }

    val rxy = safeRange(x, y)
    assert (rxy?.toString() == expectStr) { "${T::class.simpleName}: Expected: $expectStr, got $rxy" }
}

fun box(): String {
    testSafeRange('0', '1', "0..1", ::charRange)
    testSafeRange(0, 1, "0..1", ::byteRange)
    testSafeRange(0, 1, "0..1", ::shortRange)
    testSafeRange(0, 1, "0..1", ::intRange)
    testSafeRange(0L, 1L, "0..1", ::longRange)
    testSafeRange(0.0f, 1.0f, "0.0..1.0", ::floatRange)
    testSafeRange(0.0, 1.0, "0.0..1.0", ::dougleRange)
    return "OK"
}
