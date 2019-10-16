package suspendCalls

suspend fun main() {
    //Breakpoint!
    foo()
}

suspend fun foo(): Int = 42

// EXPRESSION: foo()
// RESULT: Evaluation of 'suspend' calls is not supported