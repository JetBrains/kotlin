// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

abstract class Expr<T>

class Sum<K>(val e: Expr<K>) : Expr<K?>()

private fun <V> times(e: Expr<V>, element: V): Expr<V> = TODO()

private fun <S> foo(e: Expr<S>) {}

fun test(intExpression: Expr<Int>) {
    foo(Sum(times(intExpression, 42)))
}