package ceSeveralLambdas

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

fun foo(p: () -> Int) = p()

// EXPRESSION: foo { 1 } + foo { 1 }
// RESULT: 2: I