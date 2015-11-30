// EXTRACTION_TARGET: property with initializer
val a = 1

fun foo(): String {
    val x = "xabc$a"
    val y = "${a}abcx"
    val z = "xacb$a"
    return "<selection>abc</selection>def"
}