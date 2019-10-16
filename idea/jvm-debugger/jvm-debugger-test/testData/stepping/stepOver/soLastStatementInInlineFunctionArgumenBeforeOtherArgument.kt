package soLastStatementInInlineFunctionArgumenBeforeOtherArgument

fun main(args: Array<String>) {
    bar({
        //Breakpoint!
        nop()
    }, 12)
}

inline fun bar(f: () -> Unit, a: Any) {
    nop()
    f()
}                                          // <-- Ideally this line should not be visited

fun nop() {}

// STEP_OVER: 2