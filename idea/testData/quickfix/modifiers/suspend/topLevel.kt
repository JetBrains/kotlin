// "Make bar suspend" "false"
// ACTION: Convert property initializer to getter
// ERROR: Suspend function 'foo' should be called only from a coroutine or another suspend function

suspend fun foo() = 42
val x = <caret>foo()
