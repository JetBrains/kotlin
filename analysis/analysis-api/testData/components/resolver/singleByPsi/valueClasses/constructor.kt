// LANGUAGE: +FullValueClasses

value class Pair<T>(val first: T, val second: T, val count: Int = 1)

fun test() {
    <expr>Pair(second = "second", first = "first")</expr>
}
