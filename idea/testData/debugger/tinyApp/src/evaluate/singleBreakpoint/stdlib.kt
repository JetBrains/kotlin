package stdlib

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

// EXPRESSION: array(100, 101)
// RESULT: instance of java.lang.Integer[2] (id=ID): [Ljava/lang/Integer;

// EXPRESSION: array("a", "b", "c")
// RESULT: instance of java.lang.String[3] (id=ID): [Ljava/lang/String;

// EXPRESSION: intArray(1, 2)
// RESULT: instance of int[2] (id=ID): [I

// EXPRESSION: javaClass<String>()
// RESULT: instance of java.lang.Class(reflected class=java.lang.String, id=ID): Ljava/lang/Class;

// EXPRESSION: javaClass<Int>()
// RESULT: instance of java.lang.Class(reflected class=int, id=ID): Ljava/lang/Class;

// EXPRESSION: 100.toInt()
// RESULT: 100: I

// EXPRESSION: 100.toLong()
// RESULT: 100: J

// EXPRESSION: args.sortBy { it }.size()
// RESULT: 0: I