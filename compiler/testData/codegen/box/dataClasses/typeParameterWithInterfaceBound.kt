// TARGET_BACKEND: JVM
// WITH_RUNTIME

interface A

data class B<out T : A>(val a: T)

annotation class Anno

@Anno
data class C(val a: Anno)

fun box(): String {
    val b1 = B(object : A {})
    val b2 = B(object : A {})
    if (b1.hashCode() == b2.hashCode()) return "Fail 1"
    if (b1.equals(b2)) return "Fail 2"

    val anno = C::class.java.annotations.first() as Anno
    val c1 = C(anno)
    val c2 = C(anno)
    if (c1.hashCode() != c2.hashCode()) return "Fail 3"
    if (!c1.equals(c2)) return "Fail 4"

    return "OK"
}
