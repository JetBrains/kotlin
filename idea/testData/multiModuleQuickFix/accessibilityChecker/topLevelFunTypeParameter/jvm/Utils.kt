// "Create expected function in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: Some types are not accessible from testModule_Common:,Some
// DISABLE-ERRORS

class Some

actual fun <caret>foo(some: List<Some>) {}