// "Create expected function in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: "The function declaration shouldn&#39;t have a default implementation"
// DISABLE-ERRORS

actual interface My {
    actual fun <caret>foo(param: String) {

    }
}