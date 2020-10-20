// FIR_IDENTICAL
// SKIP_TXT
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -CAST_NEVER_SUCCEEDS

fun <T> id(x: T) = x

interface Foo<out K, out V> {}

fun <K, V> fooOf(vararg pairs: Pair<K, V>) = null as Foo<K, V>

fun <A, B> to(th: A, that: B): Pair<A, B> = Pair(th, that)

class Inv<K>(x: K)

class A {
    val x: Foo<Inv<(Int) -> Unit>, Inv<(String, String, String, String) -> Unit>> =
        fooOf(
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
            to(Inv { a -> }, Inv { a, b, c, d -> }),
        )
}
