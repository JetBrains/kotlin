// FILE: soInlineLibFun.kt
package soInlineLibFun

fun main(args: Array<String>) {
    //Breakpoint!
    var i = 1

    listOf(1, 2, 3).any { it > 2 } // Step over goes into 'any' (regression)

    if (listOf(1, 2, 3).any { it > 2 }) { // Step over goes into 'any'
        i++
    }
}

inline fun test(a: () -> Unit) {
    a()
}

// STEP_OVER: 7