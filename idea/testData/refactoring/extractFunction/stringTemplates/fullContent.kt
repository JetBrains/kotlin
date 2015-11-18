// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in foo
// PARAM_TYPES: kotlin.Int, Number, Comparable<Int>, java.io.Serializable, Any
fun foo(a: Int): String {
    val x = "abc$a"
    val y = "abc${a}"
    val z = "abc{$a}def"
    return "<selection>abc$a</selection>" + "def"
}