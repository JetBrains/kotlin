class A {}

fun <T> foo(t: T) {}

fun <T> someList(): List<T> = throw Exception()

fun bar() {
    <caret>foo(someList())
}