// "Replace usages of 'old(): Unit' in whole project" "true"

@Deprecated("Use new", ReplaceWith("new<T>()"))
fun <T> old() {
}

fun <T> new() {
}

fun main() {
    <caret>old<String>()
    old<Int>()
}