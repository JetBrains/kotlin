package lambdaFun1

fun foo() {
    block {
        //Breakpoint!
        val a = 5
    }
}

fun <T> block(block: () -> T): T {
    return block()
}

fun main() {
    foo()
}

// PRINT_FRAME
// SHOW_KOTLIN_VARIABLES

// EXPRESSION: this
// RESULT: 'this' is not defined in this context