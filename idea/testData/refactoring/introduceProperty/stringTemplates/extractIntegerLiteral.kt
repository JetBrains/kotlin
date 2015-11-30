// EXTRACTION_TARGET: property with initializer
fun foo(): String {
    val x = "a1234_"
    val y = "-4123a"
    val z = "+1243a"
    val u = 123
    return "ab<selection>123</selection>def"
}