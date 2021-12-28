// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val int: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(val long: T)

inline fun <T, R> s0(x: T, fn: (Int, T) -> R) = fn(0, x)

inline fun <T, R> weirdMix(x: T, fn: (Int, T, Long, T) -> R) = fn(0, x, 0L, x)

fun <T: Int> testS0Z(x: Z<T>) = s0(x) { _, xx -> Z(xx.int + 1) }
fun <T: Long> testS0L(x: L<T>) = s0(x) { _, xx -> L(xx.long + 1L) }

fun <T: Int> testWeirdMixZ(x: Z<T>) = weirdMix(x) { _, xx, _, _ -> Z(xx.int + 1) }
fun <T: Long> testWeirdMixL(x: L<T>) = weirdMix(x) { _, xx, _, _ -> L(xx.long + 1L) }

fun box(): String {
    if (testS0Z(Z(42)).int != 43) throw AssertionError()
    if (testS0L(L(42L)).long != 43L) throw AssertionError()

    if (testWeirdMixZ(Z(42)).int != 43) throw AssertionError()
    if (testWeirdMixL(L(42L)).long != 43L) throw AssertionError()

    return "OK"
}