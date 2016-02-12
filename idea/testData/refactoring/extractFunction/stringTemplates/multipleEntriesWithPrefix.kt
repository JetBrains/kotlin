// SUGGESTED_NAMES: s, getX
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in foo
// PARAM_TYPES: kotlin.Int
fun foo(a: Int): String {
    val x = "+cd$a:${a + 1}efg"
    val y = "+cd$a${a + 1}efg"
    return "ab<selection>cd$a:${a + 1}ef</selection>"
}