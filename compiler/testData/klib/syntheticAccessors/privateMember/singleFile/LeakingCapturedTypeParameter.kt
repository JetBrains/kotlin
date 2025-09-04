// WITH_STDLIB

class A<T> {
    private fun foo(x: T) = x
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    inline fun callFoo(x: T) = foo(x)

    private fun <U> baz(x: T, y: U) = x to y
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    inline fun <U> callBaz(x: T, y: U) = baz(x, y)

    inner class B<S> {
        private fun bar(x: T, y: S) = x to y
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        inline fun callBar(x: T, y: S) = bar(x, y)
    }

    companion object Companion {
        private fun bar(x: Any) = x
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        inline fun callBar(x: Any) = bar(x)
    }

    class Nested {
        private fun bar(x: Any) = x
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        inline fun callBar(x: Any) = bar(x)
    }
}

fun box(): String {
    var res = ""
    res += A<String>().callFoo("OK1 ")
    res += A<String>().callBaz("OK2 ", "NO2 ").first
    res += A<String>().callBaz("NO3 ", "OK3 ").second
    res += A<String>().B<String>().callBar("OK4 ", "NO4 ").first
    res += A<String>().B<String>().callBar("NO5", "OK5").second
    if (res != "OK1 OK2 OK3 OK4 OK5") return res
    else return "OK"
}