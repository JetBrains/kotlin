package localFunctionCapturedLocalVariable

fun main(args: Array<String>) {
    val a = 1
    fun local() {
        //Breakpoint!
        val x = a + 2
    }
    local()
}

// EXPRESSION: a
// RESULT: 1: I
