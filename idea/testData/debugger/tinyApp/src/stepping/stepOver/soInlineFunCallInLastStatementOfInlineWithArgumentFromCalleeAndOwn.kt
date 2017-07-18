package soInlineFunCallInLastStatementOfInlineWithArgumentFromCalleeAndOwn

fun main(args: Array<String>) {
    bar {
        println("")
    }
}

inline fun bar(f2: () -> Unit) {
    //Breakpoint! (lambdaOrdinal = -1)
    foo({ 42 },
        f2)
}

inline fun foo(f1: () -> Unit, f2: () -> Unit) {
    f1()
    f2()
}

// STEP_OVER: 5