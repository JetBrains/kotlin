// DIAGNOSTICS: -UNUSED_PARAMETER

open class A
class B : A()

class Or(left: A, right: A) : A()

class Out<out T>

fun test(ls: Out<B>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("A")!>ls.reduce(::Or)<!>
}

fun <S, T : S> Out<T>.reduce(operation: (S, T) -> S): S = TODO()