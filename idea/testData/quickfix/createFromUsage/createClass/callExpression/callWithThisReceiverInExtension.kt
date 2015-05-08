// "Create class 'Foo'" "true"

class A<T>(val n: T) {

}

fun <U> A<U>.test() = this.<caret>Foo(2, "2")