// FILE: inlineFunction.kt
package inlineFunction

import inlineFunctionOtherPackage.*

fun main(args: Array<String>) {
    //Breakpoint!
    val a = 1
}

inline fun foo() = 1

// EXPRESSION: myFun { 1 }
// RESULT: 1: I

// EXPRESSION: foo()
// RESULT: 1: I

// FILE: lib.kt
package inlineFunctionOtherPackage

inline fun myFun(f: () -> Int): Int = f()

val String.prop: String
    get() {
        return "a"
    }