package frameLambdaNotUsed

fun main(args: Array<String>) {
    val val1 = 1
    foo {
        //Breakpoint!
        val a = 1
    }
}

fun foo(f: () -> Unit) {
    f()
}

// PRINT_FRAME

// EXPRESSION: val1
// RESULT: java.lang.AssertionError : Cannot find local variable: name = val1