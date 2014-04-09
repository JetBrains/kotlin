package arrays

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

// EXPRESSION: array(1, 2).map { it.toString() }
// RESULT: instance of java.util.ArrayList(id=347): Ljava/util/ArrayList;

// EXPRESSION: array(1, 2, 101, 102).filter { it > 100 }
// RESULT: instance of java.util.ArrayList(id=369): Ljava/util/ArrayList;

// EXPRESSION: array(1, 2).none()
// RESULT: 0: Z

// EXPRESSION: array(1, 2).count()
// RESULT: 2: I

// EXPRESSION: array(1, 2).size
// RESULT: 2: I

// EXPRESSION: array(1, 2).first()
// RESULT: 1: I

// EXPRESSION: array(1, 2).last()
// RESULT: 2: I

// EXPRESSION: intArray(1, 2).max()
// RESULT: instance of java.lang.Integer(id=343): Ljava/lang/Integer;

// EXPRESSION: array(1, 2).max()
// RESULT: instance of java.lang.Integer(id=343): Ljava/lang/Integer;
