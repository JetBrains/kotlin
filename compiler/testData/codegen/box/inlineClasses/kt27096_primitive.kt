// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Z1(val x: Int)
inline class Z2(val z: Z1)
inline class ZN(val z: Z1?)
inline class ZN2(val z: ZN)

fun wrap1(n: Int): Z1? = if (n < 0) null else Z1(n)
fun wrap2(n: Int): Z2? = if (n < 0) null else Z2(Z1(n))
fun wrapN(n: Int): ZN? = if (n < 0) null else ZN(Z1(n))
fun wrapN2(n: Int): ZN2? = if (n < 0) null else ZN2(ZN(Z1(n)))

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

    if (wrapN2(-1) != null) throw AssertionError()
    if (wrapN2(42) == null) throw AssertionError()
    if (wrapN2(42)!!.z.z!!.x != 42) throw AssertionError()

    return "OK"
}