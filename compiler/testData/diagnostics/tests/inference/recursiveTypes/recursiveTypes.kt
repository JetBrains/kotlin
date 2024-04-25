// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

interface RecursiveGeneric<T : RecursiveGeneric<T, U>, U>

class A : RecursiveGeneric<A, Int>
class B : RecursiveGeneric<B, Int>
class C : RecursiveGeneric<C, Unit>

fun <K> select(x: K, y: K): K = x

fun foo(a: A, b: B, c: C) {
    <!DEBUG_INFO_EXPRESSION_TYPE("RecursiveGeneric<*, kotlin.Int>")!>select(a, b)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("RecursiveGeneric<*, out kotlin.Any>")!>select(a, c)<!>
}