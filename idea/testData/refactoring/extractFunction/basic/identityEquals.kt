// SUGGESTED_NAMES: b, getT
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in test
// PARAM_DESCRIPTOR: value-parameter b: kotlin.Int defined in test
// PARAM_TYPES: kotlin.Int, kotlin.Number, kotlin.Comparable<kotlin.Int>, java.io.Serializable, kotlin.Any
// PARAM_TYPES: kotlin.Int, kotlin.Number, kotlin.Comparable<kotlin.Int>, java.io.Serializable, kotlin.Any

fun test(a: Int, b: Int): Boolean {
    val t = <selection>a === b</selection>
}