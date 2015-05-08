// "Create class 'Foo'" "true"

class A<T>(val n: T) {

}

fun test() {
    val a = A.<caret>Foo(2)
}