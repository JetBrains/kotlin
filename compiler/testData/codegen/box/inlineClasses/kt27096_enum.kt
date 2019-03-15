// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

enum class En { N, A, B, C }

inline class Z1(val x: En)
inline class Z2(val z: Z1)
inline class ZN(val z: Z1?)

fun wrap1(x: En): Z1? = if (x.ordinal == 0) null else Z1(x)
fun wrap2(x: En): Z2? = if (x.ordinal == 0) null else Z2(Z1(x))
fun wrapN(x: En): ZN? = if (x.ordinal == 0) null else ZN(Z1(x))

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