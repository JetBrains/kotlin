// "Create function 'bar'" "true"

class A<T>(val t: T)

fun <T, U> A<T>.convert(f: (T) -> U) = A(f(t))

fun foo(l: A<String>): A<Int> {
    return l.convert(fun(it: String) = <caret>bar(it))
}