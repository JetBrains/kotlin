// FILE: inlineFunctionSameLines.kt
package inlineFunctionSameLines

fun main(args: Array<String>) {
    if (1 > 2) {
        val a = 1
    }
    else {
        val b = 2
    }

    //Breakpoint!
    myFun(1)
    val a = 1
}

// FILE: inlineFunctionSameLinesDependent.kt
package inlineFunctionSameLines

inline fun <reified T> myFun(t: T): Int {
    val a = 1
    val b = 2
    val c = 3
    val d = 4
    return 1
}