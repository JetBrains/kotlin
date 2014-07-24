package frameSimple

val topVal1 = 1

fun main(args: Array<String>) {
    val val1 = 1
    val val2 = "str"
    //Breakpoint!
    val1 + topVal1
}

// PRINT_FRAME

// EXPRESSION: val1
// RESULT: 1: I

// EXPRESSION: val2
// RESULT: "str": Ljava/lang/String;

// EXPRESSION: topVal1
// RESULT: 1: I

// EXPRESSION: val1 + topVal1
// RESULT: 2: I