fun <T> emptyList(): List<T> = throw Exception()

fun <T> doSmth(l: List<T>) {}
fun doSmth(a: Any) {}

fun bar() {
    <caret>doSmth(emptyList())
}