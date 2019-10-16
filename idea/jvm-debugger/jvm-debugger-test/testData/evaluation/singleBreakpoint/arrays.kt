package arrays

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

// EXPRESSION: arrayOf(1, 2).map { it.toString() }
// RESULT: instance of java.util.ArrayList(id=ID): Ljava/util/ArrayList;

// EXPRESSION: arrayOf(1, 2, 101, 102).filter { it > 100 }
// RESULT: instance of java.util.ArrayList(id=ID): Ljava/util/ArrayList;

// EXPRESSION: arrayOf(1, 2).none()
// RESULT: 0: Z

// EXPRESSION: arrayOf(1, 2).count()
// RESULT: 2: I

// EXPRESSION: arrayOf(1, 2).size
// RESULT: 2: I

// EXPRESSION: arrayOf(1, 2).first()
// RESULT: 1: I

// EXPRESSION: arrayOf(1, 2).last()
// RESULT: 2: I

// EXPRESSION: intArrayOf(1, 2).max()
// RESULT: instance of java.lang.Integer(id=ID): Ljava/lang/Integer;

// EXPRESSION: arrayOf(1, 2).max()
// RESULT: instance of java.lang.Integer(id=ID): Ljava/lang/Integer;
