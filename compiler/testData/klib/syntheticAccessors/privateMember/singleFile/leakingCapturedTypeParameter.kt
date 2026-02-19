// WITH_STDLIB

open class A<T> {
    private fun foo(x: T) = x
    internal inline fun callFoo(x: T) = foo(x)

    private fun <U> baz(x: T, y: U) = x to y
    internal inline fun <U> callBaz(x: T, y: U) = baz(x, y)

    inner class B<S> {
        private fun barB(x: T, y: S) = x to y
        internal inline fun callBarB(x: T, y: S) = barB(x, y)
    }

    inner class C<S> private constructor(val x: S) {
        internal inline fun copy() = C<Int>(42)
    }

    companion object Companion {
        private fun barCompanion(x: Any) = x
        internal inline fun callBarCompanion(x: Any) = barCompanion(x)
    }

    class Nested {
        private fun barNested(x: Any) = x
        internal inline fun callBarNested(x: Any) = barNested(x)
    }

    inner class D : A<Int>() {
        private fun barD(x: T) = x
        internal inline fun callBarD(x: T) = barD(x)
    }

    private fun listHead(xs: List<T>): T = xs.first()
    internal inline fun callListHead(xs: List<T>) = listHead(xs)

    internal inline fun <reified R> callFooReified(x: T) = foo(x) as? R
}

class E : A<Int>() {
    private fun barE(x: Int) = x
    internal inline fun callBarE(x: Int) = barE(x)

    inner class F {
        private fun barF(x: Int) = x
        internal inline fun callBarF(x: Int) = barF(x)
    }
}

fun box(): String {
    var res = ""
    res += A<String>().callFoo("OK1 ")
    res += A<String>().callBaz("OK2 ", "NO2 ").first
    res += A<String>().callBaz("NO3 ", "OK3 ").second
    res += A<String>().B<String>().callBarB("OK4 ", "NO4 ").first
    res += A<String>().B<String>().callBarB("NO5", "OK5 ").second
    res += A<String>().D().callBarD("OK6 ")
    res += "OK" + A<String>().D().callFoo(7) + " "
    res += "OK" + A<String>().D().callBaz(8, -1).first + " "
    res += "OK" + A<String>().D().callBaz(-1, 9).second + " "
    res += "OK" + A<String>().D().B<Int>().callBarB(10, -1).first + " "
    res += "OK" + A<String>().D().B<Int>().callBarB(-1, 11).second + " "
    res += "OK" + E().callBarE(12) + " "
    res += "OK" + E().F().callBarF(13) + " "
    res += A<String>().callListHead(listOf("OK14")) + " "
    res += A<String>().callFooReified<String>("OK15")
    if (res != "OK1 OK2 OK3 OK4 OK5 OK6 OK7 OK8 OK9 OK10 OK11 OK12 OK13 OK14 OK15") return res
    else return "OK"
}