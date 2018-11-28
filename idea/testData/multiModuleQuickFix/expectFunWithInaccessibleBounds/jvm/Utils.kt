// "Create expected function in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: Cannot generate expected function: Type Some is not accessible from common code
// DISABLE-ERRORS

interface Some

actual fun <T : Some> <caret>foo(some: List<T>) {}