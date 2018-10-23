// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Z(val int: Int)
inline class L(val long: Long)

inline fun <T, R> s0(x: T, fn: (Int, T) -> R) = fn(0, x)

inline fun <T, R> weirdMix(x: T, fn: (Int, T, Long, T) -> R) = fn(0, x, 0L, x)

fun testS0Z(x: Z) = s0(x) { _, xx -> Z(xx.int + 1) }
fun testS0L(x: L) = s0(x) { _, xx -> L(xx.long + 1L) }

fun testWeirdMixZ(x: Z) = weirdMix(x) { _, xx, _, _ -> Z(xx.int + 1) }
fun testWeirdMixL(x: L) = weirdMix(x) { _, xx, _, _ -> L(xx.long + 1L) }

fun box(): String {
    if (testS0Z(Z(42)).int != 43) throw AssertionError()
    if (testS0L(L(42L)).long != 43L) throw AssertionError()

    if (testWeirdMixZ(Z(42)).int != 43) throw AssertionError()
    if (testWeirdMixL(L(42L)).long != 43L) throw AssertionError()

    return "OK"
}