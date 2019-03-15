// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Str(val string: String)
inline class Obj(val obj: Any)

inline fun <T, R> s0(x: T, fn: (Int, T) -> R) = fn(0, x)

inline fun <T, R> weirdMix(x: T, fn: (Int, T, Long, T) -> R) = fn(0, x, 0L, x)

fun testS0Str(x: Str) = s0(x) { _, xx -> Str(xx.string + "123") }
fun testS0Any(x: Obj) = s0(x) { _, xx -> Obj(xx.obj.toString() + "123") }

fun testWeirdMixStr(x: Str) = weirdMix(x) { _, xx, _, _ -> Str(xx.string + "123") }
fun testWeirdMixAny(x: Obj) = weirdMix(x) { _, xx, _, _ -> Obj(xx.obj.toString() + "123") }

fun box(): String {
    if (testS0Str(Str("abc")).string != "abc123") throw AssertionError()
    if (testS0Any(Obj("abc")).obj != "abc123") throw AssertionError()

    if (testWeirdMixStr(Str("abc")).string != "abc123") throw AssertionError()
    if (testWeirdMixAny(Obj("abc")).obj != "abc123") throw AssertionError()

    return "OK"
}