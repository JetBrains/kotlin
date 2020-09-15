// FILE: soInlineLibFunInWhen.kt
package soInlineLibFunInWhen

fun main(args: Array<String>) {
    //Breakpoint!
    val l = listOf(1, 2, 3)

    when {
        l.any { it > 4 } -> nop()
        l.any { it > 2 } -> nop()
    }
}

fun nop() {}

// STEP_OVER: 7
// Note: on the old JVM backend, debugger incorrectly steps into Collections.kt even though we're stepping over it. This is fixed in JVM_IR.
