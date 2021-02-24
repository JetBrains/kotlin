// FIR_IDENTICAL
// SKIP_TXT
// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS

fun <T> id(x: T) = x

interface Foo<out K, out V> {}

fun <K, V> fooOf(vararg pairs: Pair<K, V>) = null as Foo<K, V>

fun <A, B> to(th: A, that: B): Pair<A, B> = Pair(th, that)

class Inv<K>(x: K)

fun <A: (Int, Int, Int, Int) -> Unit> take(x: A, y: A) = null as (Int, String, String, String) -> Unit

class A {
    val x: Foo<Inv<(Int, String, String, String) -> Unit>, Inv<(Int, String, String, String) -> Unit>> =
        fooOf(
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv(take({ a, b, c, d -> }, { a, b, c, d -> }))),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv(take({ a, b, c, d -> }, { a, b, c, d -> }))),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv(take({ a, b, c, d -> }, { a, b, c, d -> }))),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv(take({ a, b, c, d -> }, { a, b, c, d -> }))),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv(take({ a, b, c, d -> }, { a, b, c, d -> }))),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
            to(Inv(take({ a, b, c, d -> }, { a, b, c, d -> })), Inv { a, b, c, d -> }),
        )
}
