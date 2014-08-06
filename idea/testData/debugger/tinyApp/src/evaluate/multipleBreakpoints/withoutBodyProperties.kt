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

class A {
    {
        // EXPRESSION: i
        // RESULT: 1: I

        // EXPRESSION: i
        // RESULT: 2: I
        for (i in 1..2) {
            //Breakpoint!
            val a = 1
        }
    }

    // EXPRESSION: 1 + 4
    // RESULT: 5: I
    //Breakpoint!
    val prop = 1
}

fun main(args: Array<String>) {
    a
    aDelegate = 1
    aLambda

    A().prop
}