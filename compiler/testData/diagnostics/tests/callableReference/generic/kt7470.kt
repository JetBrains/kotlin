// !DIAGNOSTICS: -UNUSED_VARIABLE


fun <T> shuffle(x: List<T>): List<T> = x

fun bar() {
    val s: (List<String>) -> List<String> = ::shuffle
}