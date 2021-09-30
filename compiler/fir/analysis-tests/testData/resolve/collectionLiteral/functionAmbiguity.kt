

fun <T> Int.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Int, T>.() -> Unit = {}) : Int {
    return TODO()
}

fun <T> Double.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Double, T>.() -> Unit = {}) : Double {
    return TODO()
}

fun fOver(a: Int) {}
fun fOver(a: Double) {}

fun main() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>fOver<!>([1, 2, 3])
}