// "Wrap element with 'arrayOf' call" "false"
// ERROR: Type mismatch: inferred type is String but Array<String> was expected
// ACTION: Add arrayOf wrapper
// ACTION: Change parameter 'value' type of primary constructor of class 'Foo' to 'String'
// ACTION: Create test
// ACTION: Make internal
// ACTION: Make private
// ACTION: To raw string literal
// ACTION: Wrap with []

annotation class Foo(val value: Array<String>)

@Foo(value = "abc"<caret>)
class Bar