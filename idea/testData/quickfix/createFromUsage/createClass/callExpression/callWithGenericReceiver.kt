// "Create class 'Foo'" "true"

class A<T>(val n: T) {

}

fun test<U>(u: U) {
    val a = A(u).<caret>Foo(u)
}