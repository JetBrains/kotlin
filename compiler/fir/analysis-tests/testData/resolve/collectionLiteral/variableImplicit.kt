
fun <T> Int.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Int, T>.() -> Unit = {}) : Int {
    return TODO()
}

fun <T> Double.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Double, T>.() -> Unit = {}) : Double {
    return TODO()
}


fun main() {
    val a = <!NO_BUILDERS_FOR_COLLECTION_LITERAL!>[1, 2, 3]<!>
}