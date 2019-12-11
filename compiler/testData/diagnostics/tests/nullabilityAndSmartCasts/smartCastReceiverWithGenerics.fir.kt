fun test(a: Any?) {
    if (a != null) {
        a.foo(11)
    }
}

fun <T> Any.foo(t: T) = t