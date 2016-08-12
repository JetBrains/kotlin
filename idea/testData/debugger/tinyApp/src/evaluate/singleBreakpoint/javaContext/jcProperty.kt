package jcProperty

fun main(args: Array<String>) {
    val javaClass = forTests.javaContext.JavaClass()
    //Breakpoint!
    javaClass.property()
}

// STEP_INTO: 1

// EXPRESSION: this.javaProperty
// RESULT: 1: I

// EXPRESSION: javaProperty
// RESULT: 1: I

// EXPRESSION: javaPrivateProperty
// RESULT: 1: I