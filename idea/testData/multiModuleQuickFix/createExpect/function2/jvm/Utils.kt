// "Create expected function in common module testModule_Common" "true"
// DISABLE-ERRORS

actual fun <caret>foo(i: Int, d: Double, s: String) = s == "$i$d"