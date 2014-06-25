package vars

fun main(args: Array<String>) {
    var a = 1
    a += 1
    //Breakpoint!
    args.size
}

// EXPRESSION: a
// RESULT: 2: I

// EXPRESSION: a += 1
// RESULT: 3: I

// EXPRESSION: a
// RESULT: 2: I
