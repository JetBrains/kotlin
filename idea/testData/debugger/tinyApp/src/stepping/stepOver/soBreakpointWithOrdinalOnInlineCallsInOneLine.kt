package soBreakpointWithOrdinalOnInlineCallsInOneLine

fun test(i: Int): Boolean {
    return false
}

fun foo() {}

fun main(args: Array<String>) {
    val listOf = listOf(1)
    //Breakpoint! (lambdaOrdinal = -1)
    val testSome = listOf.filterNot({test(it)}).get(0)
    foo()
}


// STEP_OVER: 4








































