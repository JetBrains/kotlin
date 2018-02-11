package soInlineCallsInOneLine

fun test(i: Int): Boolean {
    return false
}

fun foo() {}

fun main(args: Array<String>) {
    //Breakpoint!
    val listOf = listOf(1)
    val testSome = listOf.filterNot(::test)
    foo()
}


// STEP_OVER: 3








































