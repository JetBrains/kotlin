// "Create expected class in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: You cannot create the expect declaration from:,class A
// DISABLE-ERRORS

interface Some

actual class <caret>A<T : Some>