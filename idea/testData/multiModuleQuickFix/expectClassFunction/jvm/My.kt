// "Create expected function in common module testModule_Common" "true"
// DISABLE-ERRORS

actual class My {
    actual fun <caret>foo(param: String) = param.length
}