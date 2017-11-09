package soInlineCallInLastStatementInInlineDex

fun main(args: Array<String>) {
    bar()
    nop()               // 3
}                       // 4

inline fun bar() {
    //Breakpoint!
    nop()               // 1
    foo { 42 }          // 2
}

inline fun foo(f: () -> Unit) {
    f()
}

fun nop() {}

// STEP_OVER: 8