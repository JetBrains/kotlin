// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z1<T: String?>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z2<T: Z1<String?>>(val z: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ZN<T: Z1<String?>?>(val z: T)

fun wrap1(x: String): Z1<String?>? = if (x.length == 0) null else Z1(x)
fun wrap2(x: String): Z2<Z1<String?>>? = if (x.length == 0) null else Z2(Z1(x))
fun wrapN(x: String): ZN<Z1<String?>?>? = if (x.length == 0) null else ZN(Z1(x))

fun box(): String {
    if (wrap1("") != null) throw AssertionError()
    if (wrap1("a") == null) throw AssertionError()
    if (wrap1("a")!!.x != "a") throw AssertionError()

    if (wrap2("") != null) throw AssertionError()
    if (wrap2("a") == null) throw AssertionError()
    if (wrap2("a")!!.z.x != "a") throw AssertionError()

    if (wrapN("") != null) throw AssertionError()
    if (wrapN("a") == null) throw AssertionError()
    if (wrapN("a")!!.z!!.x != "a") throw AssertionError()

    return "OK"
}