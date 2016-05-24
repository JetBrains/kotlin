// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in foo
// PARAM_TYPES: kotlin.Int, kotlin.Number, kotlin.Comparable<kotlin.Int>, java.io.Serializable, kotlin.Any
fun foo(a: Int): String {
    val x = "abc$a"
    val y = "abc${a}"
    val z = "abc{$a}def"
    return "<selection>abc$a</selection>" + "def"
}