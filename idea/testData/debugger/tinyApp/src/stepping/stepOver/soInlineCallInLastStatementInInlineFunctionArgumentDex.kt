package soInlineCallInLastStatementInInlineFunctionArgumentDex

fun main(args: Array<String>) {
    bar {
        nop()
        //Breakpoint!
        foo()                     // <-- Should not stop here twice
    }
}

inline fun bar(f: () -> Unit) {
    nop()
    f()
}

inline fun foo() {
    nop()
}

fun nop() {}

// STEP_OVER: 5