package jcBlock

fun main(args: Array<String>) {
    val javaClass = forTests.javaContext.JavaClass()
    //Breakpoint!
    javaClass.block()
}

// STEP_INTO: 1
// STEP_OVER: 2

// EXPRESSION: bodyVal
// RESULT: 1: I

// EXPRESSION: thenVal
// RESULT: 1: I

// EXPRESSION: elseVal
// RESULT: Unresolved reference: elseVal