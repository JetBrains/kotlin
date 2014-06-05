class A {}

fun <T> foo(t: T) {}

fun <T> emptyList(): MutableList<T> = throw Exception()

fun bar() {
    <caret>foo(emptyList())
}