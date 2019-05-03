// "Replace with safe (?.) call" "false"
// ACTION: Convert to block body
// ACTION: Replace overloaded operator with function call
// ACTION: Wrap with '?.let { ... }' call
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Reference has a nullable type '(String.() -> Unit)?', use explicit '?.invoke()' to make a function-like call instead

fun foo(exec: (String.() -> Unit)?) = "".exec<caret>()