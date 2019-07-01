// "Create function 'bar'" "true"
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

class A<T>(val t: T)

fun <T, U> A<T>.convert(f: (T) -> U) = A(f(t))

fun foo(l: A<String>): A<Int> {
    return l.convert { <caret>bar(it) }
}