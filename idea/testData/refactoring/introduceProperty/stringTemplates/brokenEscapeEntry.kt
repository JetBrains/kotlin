// EXTRACTION_TARGET: property with initializer
val a = 1

fun foo(): String {
    return "<selection>abc$a\</selection>ndef"
}