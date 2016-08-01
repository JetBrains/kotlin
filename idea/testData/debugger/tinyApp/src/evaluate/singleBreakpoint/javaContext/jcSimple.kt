package jcSimple

fun main(args: Array<String>) {
    val javaClass = forTests.javaContext.JavaClass()
    //Breakpoint!
    javaClass.simple()
}

// STEP_INTO: 1

// EXPRESSION: 1 + 1
// RESULT: 2: I