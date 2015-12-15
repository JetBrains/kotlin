class A<X> {
    fun foo<<caret>Y>(x: X, y: Y) {

    }
}

fun bar(a: A<String>) {
    a.foo<Int>("123", 123)
}