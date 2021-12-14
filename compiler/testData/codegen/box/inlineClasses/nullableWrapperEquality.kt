// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z1(val x: String)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ZN(val z: Z1?)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ZN2(val z: ZN)

fun zap(b: Boolean): ZN2? = if (b) null else ZN2(ZN(null))

fun eq(a: Any?, b: Any?) = a == b

fun box(): String {
    val x = zap(true)
    val y = zap(false)
    if (eq(x, y)) throw AssertionError()

    return "OK"
}