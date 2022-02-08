// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z1(val x: Int?)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z2(val z: Z1)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ZN(val z: Z1?)

fun wrap1(n: Int): Z1? = if (n < 0) null else Z1(n)
fun wrap2(n: Int): Z2? = if (n < 0) null else Z2(Z1(n))
fun wrapN(n: Int): ZN? = if (n < 0) null else ZN(Z1(n))

fun box(): String {
    if (wrap1(-1) != null) throw AssertionError()
    if (wrap1(42) == null) throw AssertionError()
    if (wrap1(42)!!.x != 42) throw AssertionError()

    if (wrap2(-1) != null) throw AssertionError()
    if (wrap2(42) == null) throw AssertionError()
    if (wrap2(42)!!.z.x != 42) throw AssertionError()

    if (wrapN(-1) != null) throw AssertionError()
    if (wrapN(42) == null) throw AssertionError()
    if (wrapN(42)!!.z!!.x != 42) throw AssertionError()

    return "OK"
}