class C {
    fun <A> foo(a: Int = 1): String = "$a"

    fun <A, B> bar(a: A, b: B): String = "$a/$b"
}