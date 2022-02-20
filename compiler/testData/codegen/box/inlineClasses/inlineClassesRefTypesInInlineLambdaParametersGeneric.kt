// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Str<T: String>(val string: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Obj<T: Any>(val obj: T)

inline fun <T, R> s0(x: T, fn: (Int, T) -> R) = fn(0, x)

inline fun <T, R> weirdMix(x: T, fn: (Int, T, Long, T) -> R) = fn(0, x, 0L, x)

fun <T: String> testS0Str(x: Str<T>) = s0(x) { _, xx -> Str(xx.string + "123") }
fun <T: Any> testS0Any(x: Obj<T>) = s0(x) { _, xx -> Obj(xx.obj.toString() + "123") }

fun <T: String> testWeirdMixStr(x: Str<T>) = weirdMix(x) { _, xx, _, _ -> Str(xx.string + "123") }
fun <T: Any> testWeirdMixAny(x: Obj<T>) = weirdMix(x) { _, xx, _, _ -> Obj(xx.obj.toString() + "123") }

fun box(): String {
    if (testS0Str(Str("abc")).string != "abc123") throw AssertionError()
    if (testS0Any(Obj("abc")).obj != "abc123") throw AssertionError()

    if (testWeirdMixStr(Str("abc")).string != "abc123") throw AssertionError()
    if (testWeirdMixAny(Obj("abc")).obj != "abc123") throw AssertionError()

    return "OK"
}