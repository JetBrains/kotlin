
fun <T> List.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

fun <T> Int.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Int, T>.() -> Unit = {}) : Int {
    return TODO()
}

fun <T> Double.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Double, T>.() -> Unit = {}) : Double {
    return TODO()
}

fun main() {
    val a: List<Int> = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
}