package jcImports

fun main(args: Array<String>) {
    val javaClass = forTests.javaContext.JavaClass()
    //Breakpoint!
    javaClass.imports()
}

// STEP_INTO: 1
// STEP_OVER: 1

// EXPRESSION: list.filter { it == 1 }.size
// RESULT: 1: I