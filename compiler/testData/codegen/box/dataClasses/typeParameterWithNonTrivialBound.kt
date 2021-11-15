// TARGET_BACKEND: JVM
// WITH_STDLIB

interface A

data class B<out T : A>(val a: T)

annotation class Anno

@Anno
data class C(val a: Anno)

data class D<T : Int>(val t: T)

fun box(): String {
    val b1 = B(object : A {})
    val b2 = B(object : A {})
    if (b1.hashCode() == b2.hashCode()) return "Fail 1"
    if (b1.equals(b2)) return "Fail 2"

    val anno = C::class.java.annotations.filterIsInstance<Anno>().first()
    val c1 = C(anno)
    val c2 = C(anno)
    if (c1.hashCode() != c2.hashCode()) return "Fail 3"
    if (!c1.equals(c2)) return "Fail 4"

    val d1 = D<Int>(1)
    val d2 = D<Int>(2)
    if (d1.hashCode() == d2.hashCode()) return "Fail 5"
    if (d1.equals(d2)) return "Fail 6"

    return "OK"
}
