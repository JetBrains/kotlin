// !LANGUAGE: -NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

open class A
class B : A()

class Or(left: A, right: A) : A()

class Out<out T>

fun test(ls: Out<B>) {
    ls.reduce(::Or)
}

fun <S, T : S> Out<T>.reduce(operation: (S, T) -> S): S = TODO()
