package withoutBodyFunctions

import kotlin.properties.Delegates

// EXPRESSION: 1 + 1
// RESULT: 2: I
val aGet: Int
    //Breakpoint!
    get() = 1

// EXPRESSION: 1 + 2
// RESULT: 3: I
val aGet2: Int
    //Breakpoint!
    get() { return 1 }

fun fooWithBody(i: Int): Int {
    // EXPRESSION: i
    // RESULT: 2: I
    //Breakpoint!
    return i
}

// EXPRESSION: i
// RESULT: 2: I
//Breakpoint!
fun foo(i: Int) = i

// EXPRESSION: i
// RESULT: 2: I
//Breakpoint!
fun fooOneLine(i: Int): Int { return 1 }

// Cannot stop at this breakpoint - empty body
//Breakpoint!
fun fooEmpty(i: Int) {}

fun main(args: Array<String>) {
    aGet
    aGet2

    fooWithBody(2)
    foo(2)
    fooOneLine(2)
    fooEmpty(2)
}