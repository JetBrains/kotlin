package localVariables

fun main(args: Array<String>) {
    val aaa1 = 1
    //Breakpoint!
    val t = 2
    val aaa3 = 2
}

// EXPRESSION: aaa1
// RESULT: 1: I

// EXPRESSION: aaa3 + aaa1
// RESULT: Unresolved reference: aaa3