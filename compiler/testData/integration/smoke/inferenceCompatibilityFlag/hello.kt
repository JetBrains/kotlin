package Hello

class B<E>

fun <T> B<T>.foo(f: (T) -> Unit) {
    @Suppress("UNCHECKED_CAST")
    f(null as T)
}

fun test(b: B<out Number>) {
    b.foo {
        it.toInt()
    }

    b.foo { x ->
        x.toInt()
    }
}
