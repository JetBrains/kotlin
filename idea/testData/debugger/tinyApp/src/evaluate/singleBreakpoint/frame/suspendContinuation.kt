package suspendContinuation

suspend fun main() {
    val a = 5
    foo()
    //Breakpoint!
    val b = 4
}

suspend fun foo() {}

// PRINT_FRAME
// SHOW_KOTLIN_VARIABLES

// EXPRESSION: a
// RESULT: 5: I