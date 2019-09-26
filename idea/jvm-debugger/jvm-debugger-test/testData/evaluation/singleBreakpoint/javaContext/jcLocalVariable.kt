package jcLocalVariable

fun main(args: Array<String>) {
    val javaClass = forTests.javaContext.JavaClass()
    //Breakpoint!
    javaClass.localVariable()
}

// STEP_INTO: 1
// STEP_OVER: 1

// EXPRESSION: i
// RESULT: 1: I