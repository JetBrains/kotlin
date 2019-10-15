package frameLambdaNotAccessible

fun main() {
    val k = "lorem"
    bar()()
}

fun bar(): () -> Unit {
    val k = "ipsum"
    return {
        val l = "doloret"
        //Breakpoint!
        Unit
    }
}

// PRINT_FRAME

// EXPRESSION: k
// RESULT: 'k' is not captured