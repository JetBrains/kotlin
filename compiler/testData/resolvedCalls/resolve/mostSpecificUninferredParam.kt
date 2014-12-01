fun <T> someList(): List<T> = throw Exception()

fun <T> doSmth(l: List<T>) {}
fun doSmth(a: Any) {}

fun bar() {
    <caret>doSmth(someList())
}