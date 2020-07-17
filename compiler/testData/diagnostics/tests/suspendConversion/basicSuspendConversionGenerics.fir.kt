// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

class Inv2<T, K>

fun <T> foo1(f: suspend (T) -> String): T = TODO()
fun <T> foo2(f: suspend () -> T): T = TODO()
fun <T, K> foo3(f: suspend (T) -> K): Inv2<T, K> = TODO()

fun <I> id(e: I): I = e

fun test(f: (Int) -> String, g: () -> String) {
    val a0 = <!INAPPLICABLE_CANDIDATE!>foo1<!>(f)
    a0

    val a1 = <!INAPPLICABLE_CANDIDATE!>foo2<!>(g)
    a1

    val a2 = <!INAPPLICABLE_CANDIDATE!>foo3<!>(f)
    a2

    val a3 = <!INAPPLICABLE_CANDIDATE!>foo1<!>(id(f))
    a3

    val a4 = <!INAPPLICABLE_CANDIDATE!>foo2<!>(id(g))
    a4

    val a5 = <!INAPPLICABLE_CANDIDATE!>foo3<!>(id(f))
    a5
}
