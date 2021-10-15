
fun <T> Int.Companion.buildSeq(size: Int, init: SeqCollectionLiteralBuilder<Int, T>.() -> Unit = {}) : Int {
    return TODO()
}

fun <T> Double.Companion.buildSeq(size: Int, init: SeqCollectionLiteralBuilder<Double, T>.() -> Unit = {}) : Double {
    return TODO()
}

fun <T> fVariable(a: T) {}

fun main() {
    fVariable(<!CANT_CHOOSE_BUILDER!>[1, 2, 3]<!>)
}