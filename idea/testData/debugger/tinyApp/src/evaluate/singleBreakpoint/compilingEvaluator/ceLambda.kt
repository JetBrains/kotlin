package ceLambda

fun main(args: Array<String>) {
    val a = 1
    //Breakpoint!
    args.size
}

fun foo(p: () -> Int) = p()

// EXPRESSION: foo { 1 }
// RESULT: 1: I

// EXPRESSION: foo { a }
// RESULT: 1: I

