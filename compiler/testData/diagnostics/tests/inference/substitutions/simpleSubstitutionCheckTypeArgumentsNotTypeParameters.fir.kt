class A<T> {
    fun useT(t: T) = t

    fun <U> newA(): A<U> = A()
}

fun test1() {
    A<Int>().newA<String>().useT("")
    A<Int>().newA<String>().<!INAPPLICABLE_CANDIDATE!>useT<!>(1)
}
