// "Make bar suspend" "false"
// ACTION: Convert property initializer to getter
// ERROR: Suspend functions are only allowed to be called from a coroutine or another suspend function

suspend fun foo() = 42
val x = <caret>foo()
