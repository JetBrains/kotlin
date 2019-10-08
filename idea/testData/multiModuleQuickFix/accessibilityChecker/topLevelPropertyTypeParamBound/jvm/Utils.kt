// "Create expected property in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: Some types are not accessible from testModule_Common:,A
// DISABLE-ERRORS
interface A

actual val <T: A> Some<T>.<caret>foo: Some<T> get() = TODO()