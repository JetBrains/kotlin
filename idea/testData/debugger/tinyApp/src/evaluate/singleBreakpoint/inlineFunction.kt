package inlineFunction

import inlineFunctionOtherPackage.*

fun main(args: Array<String>) {
    //Breakpoint!
    val a = 1
}

// EXPRESSION: myFun { 1 }
// RESULT: 1: I