data class Pair<F, S>(val first: F, val second: S)

fun foo1(x: Any?) = when (x) {
    is Pair -> 0
    is Pair<*, *> -> 0
    is Pair<Int, Int> -> 0
    is Pair() -> 0
    is Pair<*, *>() -> 0
    is Pair<Int, Int>() -> 0
    is Pair(val a: Pair) -> 0
    is Pair(val a: Pair<*, *>) -> 0
    is Pair(val a: Pair<Int, Int>) -> 0
    is Pair(val a = Pair()) -> 0
    is Pair(val a = Pair<*, *>()) -> 0
    is Pair(val a = Pair<Int, Int>()) -> 0
    else -> 0
}

fun foo2(x: Pair<Pair<Int, Int>, Any?>) = when (x) {
    is Pair -> 0
    is Pair<*, *> -> 0
    is Pair<Int, Int> -> 0
    is Pair() -> 0
    is Pair<*, *>() -> 0
    is Pair<Int, Int>() -> 0
    is Pair(val a: Pair) -> 0
    is Pair(val a: Pair<*, *>) -> 0
    is Pair(val a: Pair<Int, Int>) -> 0
    is Pair(val a = Pair()) -> 0
    is Pair(val a = Pair<*, *>()) -> 0
    is Pair(val a = Pair<Int, Int>()) -> 0
    else -> 0
}