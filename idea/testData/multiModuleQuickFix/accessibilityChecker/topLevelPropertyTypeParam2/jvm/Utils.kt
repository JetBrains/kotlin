// "Create expected property in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: Some types are not accessible from testModule_Common:,Some
// DISABLE-ERRORS

class Some<T>

actual val <T>Some<T>.<caret>foo: Some<T> get() = TODO()