// "Create member property 'A.foo'" "true"

class A<T>(val n: T)

fun test() {
    A(1).<caret>foo = "1"
}
