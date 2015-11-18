// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in foo
// PARAM_TYPES: kotlin.Int, Number, Comparable<Int>, java.io.Serializable, Any
fun foo(a: Int): String {
    val x = "-$a"
    val y = "x${a}y"
    val z = "x$ay"
    return "abc<selection>${a}</selection>def"
}