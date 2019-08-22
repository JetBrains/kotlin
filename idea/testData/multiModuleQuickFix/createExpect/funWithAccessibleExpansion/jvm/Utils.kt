// "Create expected function in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: You cannot create the expect declaration from:,fun foo() = &quot;&quot;
// DISABLE-ERRORS

typealias SomeString = String

actual fun <caret>foo(): SomeString = ""