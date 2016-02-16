package inlineFunctionBreakpointAnotherFile

fun main(args: Array<String>) {
    val a1 = 1
    val a2 = 1
    val a3 = 1

    inlineFunctionWithBreakpoint.myFun {
        val a = 1
    }
}

// ADDITIONAL_BREAKPOINT: inlineFunctionWithBreakpoint.kt:inline fun myFun