// "Create class 'Foo'" "true"

class A<T>(val n: T) {

}

fun test() {
    val a = A(1).<caret>Foo(2)
}