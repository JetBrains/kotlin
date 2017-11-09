// "Change 'y' to '*y'" "false"
// ACTION: Add 'toString()' call
// ACTION: Change parameter 'x' type of function 'foo' to 'Array<String>'
// ACTION: Convert to block body
// ACTION: Create function 'foo'
// ERROR: Type mismatch: inferred type is Array<String> but String was expected

fun foo(x: String) {}

fun bar(y: Array<String>) = foo(y<caret>)
