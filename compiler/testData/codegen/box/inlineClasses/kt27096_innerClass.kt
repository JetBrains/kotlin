// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

class Outer<X>(val x: X) {
    inner class Inner<Y>(val y: Y) {
        val hasNull = x == null || y == null

        fun outerX() = x

        override fun equals(other: Any?): Boolean =
            other is Outer<*>.Inner<*> &&
                    other.outerX() == x &&
                    other.y == y
    }
}

inline class Z1<X, Y>(val x: Outer<X>.Inner<Y>)
inline class Z2<X, Y>(val z: Z1<X, Y>)
inline class ZN<X, Y>(val z: Z1<X, Y>?)

fun <X, Y> wrap1(xy : Outer<X>.Inner<Y>): Z1<X, Y>? = if (xy.hasNull) null else Z1(xy)
fun <X, Y> wrap2(xy : Outer<X>.Inner<Y>): Z2<X, Y>? = if (xy.hasNull) null else Z2(Z1(xy))
fun <X, Y> wrapN(xy : Outer<X>.Inner<Y>): ZN<X, Y>? = if (xy.hasNull) null else ZN(Z1(xy))

fun box(): String {
    val n = Outer(null).Inner("a")
    val a = Outer("a").Inner("a")

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