// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
class B<E>

fun <T> B<T>.foo(f: (T) -> Unit) {}
fun <T> B<T>.bar(f: (T, T) -> Unit, g: (T, T) -> Unit) {}
fun <T> B<T>.baz(f: (B<T>) -> Unit) {}

fun number(x: Number) {}
fun Number.foobar() {}

fun test(b: B<out Number>) {
    b.foo {
        it checkType { _<Number>() }
        it.toInt()
    }

    b.foo { x ->
        x checkType { _<Number>() }
        x.toInt()
    }

    b.bar({ x, y ->
        x checkType { _<Number>() }
        y checkType { _<Number>() }
        x.toInt()
        y.toInt()
    }) { u, w ->
        u checkType { _<Number>() }
        w checkType { _<Number>() }

        u.toInt()
        w.toInt()
    }

    b.foo(::number)
    b.foo(Number::foobar)

    b.baz {
        b -> b checkType { _<B<out Number>>() }
    }
}
