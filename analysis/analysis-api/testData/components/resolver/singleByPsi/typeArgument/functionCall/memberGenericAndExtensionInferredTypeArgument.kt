class C {
    fun <T> foo(t: T) {}
}

fun C.foo(s: CharSequence) {}

fun test(c: C) {
    <expr>c.foo("x")</expr>
}
