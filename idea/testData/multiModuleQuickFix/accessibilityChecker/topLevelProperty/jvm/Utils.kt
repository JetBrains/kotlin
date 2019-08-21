// "Create expected property in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: You cannot create the expect declaration from:,actual val foo: Some = TODO()
// DISABLE-ERRORS

interface Some

actual val foo<caret>: Some = TODO()