// "Make bar suspend" "false"
// ERROR: Suspend functions are only allowed to be called from a coroutine or another suspend function

suspend fun foo() {}

class My {
    init {
        <caret>foo()
    }
}
