package frameInnerLambda

fun main(args: Array<String>) {
    val val1 = 1
    foo {
        val val2 = 1
        foo {
            //Breakpoint!
            val1 + val2
        }
    }
}

fun foo(f: () -> Unit) {
    f()
}

// PRINT_FRAME

// EXPRESSION: val1
// RESULT: 1: I

// EXPRESSION: val2
// RESULT: 1: I

// EXPRESSION: val1 + val2
// RESULT: 2: I