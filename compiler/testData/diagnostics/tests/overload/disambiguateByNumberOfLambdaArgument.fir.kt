// WITH_STDLIB
// ISSUE: KT-41013

fun <V, R> Iterable<V>.map(transform: (V) -> R): List<R> {
    return this.mapIndexed { i, v -> transform(v) }
}
fun <V, R> Iterable<V>.map(transform: (V, Int) -> R): List<R> {
    return this.mapIndexed { i, v -> transform(v, i) }
}

fun main() {
    val list = listOf(1, 2, 3)
    println(list.map { v -> v })
    println(list.map { it })
}
