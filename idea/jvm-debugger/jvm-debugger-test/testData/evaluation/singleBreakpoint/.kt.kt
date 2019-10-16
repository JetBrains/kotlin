// Test evaluate expression for default package

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

class Test(val a: Int)

// EXPRESSION: 1 + 1
// RESULT: 2: I

// EXPRESSION: Test(1).a
// RESULT: 1: I
