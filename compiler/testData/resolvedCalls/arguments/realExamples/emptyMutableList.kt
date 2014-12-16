class A {}

fun <T> foo(t: T) {}

fun <T> someList(): MutableList<T> = throw Exception()

fun bar() {
    <caret>foo(someList())
}