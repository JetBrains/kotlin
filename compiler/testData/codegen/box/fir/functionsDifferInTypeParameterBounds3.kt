interface A
interface B
interface C
interface D
interface Inv<T>

open class K

interface X {
    fun <T> foo(t: T): String where T : A, T : B, T : C, T : D
    fun <T> foo(t: T): String where T : K, T : Inv<B>, T : A?
    fun <T> foo(t: T): String where T : Inv<out Inv<in C>>, T : Any
}

class Y : X {
    override fun <T> foo(t: T): String where T : A, T : C, T : B, T : D = "1"
    override fun <T> foo(t: T): String where T : K, T : A?, T : Inv<B> = "2"
    override fun <T> foo(t: T): String where T : Any, T : Inv<out Inv<in C>> = "3"
}

fun box(): String {
    val abcd = object : A, B, C, D {}
    val kab = object : K(), A, Inv<B> {}
    val iic = object : Inv<Inv<in C>> {}
    val y = Y()

    if (y.foo(abcd) != "1") return "Fail 1"
    if (y.foo(kab) != "2") return "Fail 2"
    if (y.foo(iic) != "3") return "Fail 3"

    return "OK"
}
