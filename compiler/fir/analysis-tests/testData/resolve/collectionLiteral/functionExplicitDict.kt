
fun <K, V> Set.Companion.buildDict(size: Int, init: DictCollectionLiteralBuilder<Set<V>, K, V>.() -> Unit = {}): Set<V> {
    return TODO()
}

fun <K, V> List.Companion.buildDict(size: Int, init: DictCollectionLiteralBuilder<List<V>, K, V>.() -> Unit = {}): List<V> {
    return TODO()
}

fun <K, V> Int.Companion.buildDict(size: Int, init: DictCollectionLiteralBuilder<Int, K, V>.() -> Unit = {}) : Int {
    return TODO()
}

fun <K, V> Double.Companion.buildDict(size: Int, init: DictCollectionLiteralBuilder<Double, K, V>.() -> Unit = {}) : Double {
    return TODO()
}

fun f(set: Set<Int>) {}
fun b(a: Int) {}

fun main() {
    f(["1": 1, "2": 2, "3": 3])
    b(["1": 1, "2": 2, "3": 3])
}