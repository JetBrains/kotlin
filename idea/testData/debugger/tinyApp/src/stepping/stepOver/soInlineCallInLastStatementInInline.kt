package soInlineCallInLastStatementInInline

fun main(args: Array<String>) {
    bar()
}

inline fun bar() {
    //Breakpoint!
    val a = 1
    foo { 42 }
}

inline fun foo(f: () -> Unit) {
    f()
}

// STEP_OVER: 3