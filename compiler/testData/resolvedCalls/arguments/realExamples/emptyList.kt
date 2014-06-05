class A {}

fun <T> foo(t: T) {}

fun <T> emptyList(): List<T> = throw Exception()

fun bar() {
    <caret>foo(emptyList())
}