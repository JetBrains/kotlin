interface I<T> {
    @Experimental
    val x: T
}

fun <S> foo(x: I<S>) {
    if (x is A) {
        val a = x.x // OPT_IN_USAGE_ERROR
    }
}

fun foo2(x: A) {
    if (x is I<*>) {
        val a = x.x // OPT_IN_USAGE_ERROR
    }
}

fun main() {
    foo(object : A(), I<CharSequence> {})
    foo2(object : A(), I<CharSequence> {})
}
