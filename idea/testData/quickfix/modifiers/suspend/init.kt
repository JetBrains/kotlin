// "Make bar suspend" "false"
// ACTION: Introduce import alias
// ERROR: Suspend function 'foo' should be called only from a coroutine or another suspend function

suspend fun foo() {}

class My {
    init {
        <caret>foo()
    }
}
