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

inline fun <R> withDefaultZ(fn: (Z<Int>) -> R, x: Z<Int> = Z(42)) = fn(x)
inline fun <R> withDefaultL(fn: (L<Long>) -> R, x: L<Long> = L(42L)) = fn(x)
inline fun <R> withDefaultL2(x: L<Long> = L(42L), fn: (L<Long>) -> R) = fn(x)
inline fun <R> withDefaultStr(fn: (Str<String>) -> R, x: Str<String> = Str("abc")) = fn(x)
inline fun <R> withDefaultObj(fn: (Obj<Any>) -> R, x: Obj<Any> = Obj("abc")) = fn(x)
inline fun <R> withDefaultObj2(x: Obj<Any> = Obj("abc"), fn: (Obj<Any>) -> R) = fn(x)

fun testWithDefaultZ() = withDefaultZ({ Z(it.int + 1) })
fun testWithDefaultL() = withDefaultL({ L(it.long + 1L) })
fun testWithDefaultL2() = withDefaultL2(fn = { L(it.long + 1L) })
fun testWithDefaultStr() = withDefaultStr({ Str(it.string + "1") })
fun testWithDefaultObj() = withDefaultObj({ Obj(it.obj.toString() + "1") })
fun testWithDefaultObj2() = withDefaultObj2(fn = { Obj(it.obj.toString() + "1") })

fun box(): String {
    if (testWithDefaultZ().int != 43) throw AssertionError()
    if (testWithDefaultL().long != 43L) throw AssertionError()
    if (testWithDefaultL2().long != 43L) throw AssertionError()
    if (testWithDefaultStr().string != "abc1") throw AssertionError()
    if (testWithDefaultObj().obj != "abc1") throw AssertionError()
    if (testWithDefaultObj2().obj != "abc1") throw AssertionError()

    return "OK"
}