// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val int: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(val long: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Str<T: String>(val string: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Obj<T: Any>(val obj: T)

inline fun <R> s1Z(x: Z<Int>, fn: (Int, Z<Int>) -> R) = fn(1, x)
inline fun <R> s1L(x: L<Long>, fn: (Int, L<Long>) -> R) = fn(1, x)
inline fun <R> s1Str(x: Str<String>, fn: (Int, Str<String>) -> R) = fn(1, x)
inline fun <R> s1Obj(x: Obj<Any>, fn: (Int, Obj<Any>) -> R) = fn(1, x)

fun testS1Z(a: Z<Int>) = s1Z(a) { i, xx -> Z(xx.int + i) }
fun testS1L(a: L<Long>) = s1L(a) { i, xx -> L(xx.long + i.toLong()) }
fun testS1Str(a: Str<String>) = s1Str(a) { i, xx -> Str(xx.string + i.toString()) }
fun testS1Obj(a: Obj<Any>) = s1Obj(a) { i, xx -> Obj(xx.obj.toString() + i.toString()) }

fun box(): String {
    if (testS1Z(Z(42)).int != 43) throw AssertionError()
    if (testS1L(L(42L)).long != 43L) throw AssertionError()
    if (testS1Str(Str("abc")).string != "abc1") throw AssertionError()
    if (testS1Obj(Obj("abc")).obj != "abc1") throw AssertionError()

    return "OK"
}