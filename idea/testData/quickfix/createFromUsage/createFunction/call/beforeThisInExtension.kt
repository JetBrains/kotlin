// "Create function 'foo' from usage" "true"

class A<T>(val n: T)

fun <U> A<U>.test(): A<Int> {
    return this.<caret>foo(2, "2")
}