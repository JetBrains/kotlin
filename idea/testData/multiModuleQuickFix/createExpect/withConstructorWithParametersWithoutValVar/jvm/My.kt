// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

actual class My<caret> actual constructor(actual val a: Int, b: String) {
    actual fun foo(param: String) = param.length
}