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