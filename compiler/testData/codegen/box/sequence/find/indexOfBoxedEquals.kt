// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

class Asymmetric(val id: Int) {
    // equal to any `Asymmetric` only when this is the searched element, like a non-symmetric `equals`
    override fun equals(other: Any?): Boolean = id == 0
    override fun hashCode(): Int = 0
}

fun box(): String {
    val nan = sequenceOf(1.0, Double.NaN).indexOf(Double.NaN)
    if (nan != 1) return "Fail: NaN index=$nan"
    val zero = sequenceOf(-0.0).indexOf(0.0)
    if (zero != -1) return "Fail: -0.0 index=$zero"
    val floatNan = sequenceOf(Float.NaN).map { it }.indexOf(Float.NaN)
    if (floatNan != 0) return "Fail: Float.NaN index=$floatNan"
    // the stdlib calls `element == item`, so `equals` of the searched element decides
    val searching = Asymmetric(0)
    val other = Asymmetric(1)
    val asymmetric = sequenceOf(other).indexOf(searching)
    if (asymmetric != 0) return "Fail: asymmetric index=$asymmetric"
    val asymmetricReversed = sequenceOf(searching).indexOf(other)
    if (asymmetricReversed != -1) return "Fail: asymmetric reversed index=$asymmetricReversed"
    return "OK"
}
