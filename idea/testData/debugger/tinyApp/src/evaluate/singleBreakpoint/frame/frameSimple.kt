package frameSimple

val topVal1 = 1

fun main(args: Array<String>) {
    val val1 = 1
    val val2 = MyClass()
    //Breakpoint!
    val1 + topVal1
}

class MyClass

// PRINT_FRAME

// EXPRESSION: val1
// RESULT: 1: I

// EXPRESSION: val2
// RESULT: instance of frameSimple.MyClass(id=ID): LframeSimple/MyClass;

// EXPRESSION: topVal1
// RESULT: 1: I

// EXPRESSION: val1 + topVal1
// RESULT: 2: I