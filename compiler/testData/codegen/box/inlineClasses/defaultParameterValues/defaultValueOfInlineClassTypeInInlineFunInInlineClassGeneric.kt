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

OPTIONAL_JVM_INLINE_ANNOTATION
value class Host<T: Int>(val xx: T) {
    inline fun <R> withDefaultZ(fn: (Z<Int>) -> R, x: Z<Int> = Z(xx)) = fn(x)
    inline fun <R> withDefaultL(fn: (L<Long>) -> R, x: L<Long> = L(xx.toLong())) = fn(x)
    inline fun <R> withDefaultL2(x: L<Long> = L(xx.toLong()), fn: (L<Long>) -> R) = fn(x)
    inline fun <R> withDefaultStr(fn: (Str<String>) -> R, x: Str<String> = Str(xx.toString())) = fn(x)
    inline fun <R> withDefaultObj(fn: (Obj<Any>) -> R, x: Obj<Any> = Obj(xx.toString())) = fn(x)
    inline fun <R> withDefaultObj2(x: Obj<Any> = Obj(xx.toString()), fn: (Obj<Any>) -> R) = fn(x)

    fun testWithDefaultZ() = withDefaultZ({ Z(it.int + 1) })
    fun testWithDefaultL() = withDefaultL({ L(it.long + 1L) })
    fun testWithDefaultL2() = withDefaultL2(fn = { L(it.long + 1L) })
    fun testWithDefaultStr() = withDefaultStr({ Str(it.string + "1") })
    fun testWithDefaultObj() = withDefaultObj({ Obj(it.obj.toString() + "1") })
    fun testWithDefaultObj2() = withDefaultObj2(fn = { Obj(it.obj.toString() + "1") })
}

fun box(): String {
    val h = Host(42)
    if (h.testWithDefaultZ().int != 43) throw AssertionError()
    if (h.testWithDefaultL().long != 43L) throw AssertionError()
    if (h.testWithDefaultL2().long != 43L) throw AssertionError()
    if (h.testWithDefaultStr().string != "421") throw AssertionError()
    if (h.testWithDefaultObj().obj != "421") throw AssertionError()
    if (h.testWithDefaultObj2().obj != "421") throw AssertionError()

    return "OK"
}