// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z1<T: String>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ZN<T: Z1<String>?>(val z: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ZN2<T: ZN<Z1<String>?>>(val z: T)

fun zap(b: Boolean): ZN2<ZN<Z1<String>?>>? = if (b) null else ZN2(ZN(null))

fun eq(a: Any?, b: Any?) = a == b

fun box(): String {
    val x = zap(true)
    val y = zap(false)
    if (eq(x, y)) throw AssertionError()

    return "OK"
}