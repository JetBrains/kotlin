package soInlineCallInLastStatementInInlineDex

fun main(args: Array<String>) {
    bar()
    nop()
}

inline fun bar() {
    //Breakpoint!
    nop()
    foo { 42 }
}

inline fun foo(f: () -> Unit) {
    f()
}

fun nop() {}

// STEP_OVER: 8