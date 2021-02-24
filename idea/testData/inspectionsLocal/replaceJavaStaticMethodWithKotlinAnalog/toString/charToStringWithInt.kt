// PROBLEM: none
// DISABLE-ERRORS
fun foo() {
    val codePoint = "1D41E".toInt(16)
    val t = java.lang.Character.<caret>toString(codePoint)
}