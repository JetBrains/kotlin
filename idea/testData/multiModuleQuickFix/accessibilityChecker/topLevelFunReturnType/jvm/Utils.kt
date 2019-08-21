// "Create expected function in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: You cannot create the expect declaration from:,fun foo(some: List&lt;T&gt;) = TODO()
// DISABLE-ERRORS

interface Some

actual fun <T> <caret>foo(some: List<T>): Some = TODO()