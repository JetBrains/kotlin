// EXTRACTION_TARGET: property with initializer
fun foo(): String {
    val x = "xyfalsez"
    val y = "xyFalsez"
    val z = false
    return "ab<selection>false</selection>def"
}