// EXTRACTION_TARGET: property with initializer
val a = 1

fun foo(): String {
    val x = "xdef$a"
    val y = "${a}defx"
    val z = "xddf$a"
    return "abc<selection>def</selection>"
}