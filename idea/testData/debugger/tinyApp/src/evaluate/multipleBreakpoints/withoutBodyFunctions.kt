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

// EXPRESSION: i
// RESULT: 2: I
//Breakpoint!
fun fooEmpty(i: Int) {}

object A {
    // EXPRESSION: test2()
    // RESULT: 2: I
    //Breakpoint!
    @JvmStatic fun fooWithoutBodyInsideObject() = test2()
    fun test2() = 2
}

fun main(args: Array<String>) {
    aGet
    aGet2

    fooWithBody(2)
    foo(2)
    fooOneLine(2)
    fooEmpty(2)

    A.fooWithoutBodyInsideObject()
}