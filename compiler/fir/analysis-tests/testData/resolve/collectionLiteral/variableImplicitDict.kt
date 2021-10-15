
fun <K, V> Int.Companion.buildDict(size: Int, init: DictCollectionLiteralBuilder<Int, K, V>.() -> Unit = {}) : Int {
    return TODO()
}

fun <K, V> Double.Companion.buildDict(size: Int, init: DictCollectionLiteralBuilder<Double, K, V>.() -> Unit = {}) : Double {
    return TODO()
}


fun main() {
    val a = <!NO_BUILDERS_FOR_COLLECTION_LITERAL!>[1: 1, 2: 2, 3: 3]<!>
}