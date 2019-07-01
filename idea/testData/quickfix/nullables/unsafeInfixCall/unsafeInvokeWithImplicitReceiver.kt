// "Replace with safe (?.) call" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Convert to block body
// ACTION: Replace overloaded operator with function call
// ACTION: Replace with safe (this?.) call
// ACTION: Wrap with '?.let { ... }' call
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type String?
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

fun String?.foo(exec: (String.() -> Unit)) = exec<caret>()