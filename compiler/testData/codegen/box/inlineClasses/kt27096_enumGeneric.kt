// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: JVM
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

enum class En { N, A, B, C }

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z1<T: En>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z2<T: Z1<En>>(val z: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ZN<T: Z1<En>>(val z: T?)

fun wrap1(x: En): Z1<En>? = if (x.ordinal == 0) null else Z1(x)
fun wrap2(x: En): Z2<Z1<En>>? = if (x.ordinal == 0) null else Z2(Z1(x))
fun wrapN(x: En): ZN<Z1<En>>? = if (x.ordinal == 0) null else ZN(Z1(x))

fun box(): String {
    val n = En.N
    val a = En.A

    if (wrap1(n) != null) throw AssertionError()
    if (wrap1(a) == null) throw AssertionError()
    if (wrap1(a)!!.x != a) throw AssertionError()

    if (wrap2(n) != null) throw AssertionError()
    if (wrap2(a) == null) throw AssertionError()
    if (wrap2(a)!!.z.x != a) throw AssertionError()

    if (wrapN(n) != null) throw AssertionError()
    if (wrapN(a) == null) throw AssertionError()
    if (wrapN(a)!!.z!!.x != a) throw AssertionError()

    return "OK"
}