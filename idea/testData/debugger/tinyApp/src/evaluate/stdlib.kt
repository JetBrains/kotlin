package stdlib

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

// EXPRESSION: array(100, 101)
// RESULT: instance of java.lang.Integer[2] (id=338): [Ljava/lang/Integer;

// EXPRESSION: array("a", "b", "c")
// RESULT: instance of java.lang.String[3] (id=346): [Ljava/lang/String;

// EXPRESSION: intArray(1, 2)
// RESULT: instance of int[2] (id=352): [I

// EXPRESSION: javaClass<String>()
// RESULT: instance of java.lang.Class(reflected class=java.lang.String, id=194): Ljava/lang/Class;

// EXPRESSION: javaClass<Int>()
// RESULT: instance of java.lang.Class(reflected class=int, id=355): Ljava/lang/Class;

// EXPRESSION: 100.toInt()
// RESULT: 100: I

// EXPRESSION: 100.toLong()
// RESULT: 100: J