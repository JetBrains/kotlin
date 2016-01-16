package methodWithBreakpoint

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

fun foo(): Int {
    //Breakpoint!
    return 1
}

// EXPRESSION: foo()
// RESULT: 1: I
