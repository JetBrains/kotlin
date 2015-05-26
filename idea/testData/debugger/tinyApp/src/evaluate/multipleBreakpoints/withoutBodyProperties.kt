package withoutBodyProperties

import kotlin.properties.Delegates

// EXPRESSION: 1 + 1
// RESULT: 2: I
//Breakpoint!
val a = 1

// EXPRESSION: 1 + 2
// RESULT: 3: I
//Breakpoint!
var aDelegate: Int by Delegates.notNull()

// EXPRESSION: 1 + 3
// RESULT: 4: I
//Breakpoint!
val aLambda = { 1 + 1 }

// EXPRESSION: 1 + 4
// RESULT: 5: I
//Breakpoint!
val aWoBody: Int get() = 1

// EXPRESSION: 1 + 5
// RESULT: 6: I
//Breakpoint!
val aWoBody2: Int get() { return 1 }

class A {
    init {
        // EXPRESSION: i
        // RESULT: 1: I
        for (i in 1..1) {
            //Breakpoint!
            val a = 1
        }
    }

    // EXPRESSION: 1 + 6
    // RESULT: 7: I
    //Breakpoint!
    val prop = 1

    fun test()= prop
}

fun main(args: Array<String>) {
    a
    aDelegate = 1
    aLambda
    aWoBody
    aWoBody2

    A().test()
}