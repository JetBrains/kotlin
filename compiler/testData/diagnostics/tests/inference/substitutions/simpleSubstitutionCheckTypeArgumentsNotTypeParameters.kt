class A<T> {
    fun useT(t: T) = t

    fun <U> newA(): A<U> = A()
}

fun test1() {
    A<Int>().newA<String>().useT("")
    A<Int>().newA<String>().useT(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
}
