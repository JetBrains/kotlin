inline fun f<caret>oo(block: () -> Unit) {
    contract {
        callsInPlace(<expr>block</expr>, InvocationKind.EXACTLY_ONCE)
    }

    before()
    <expr_1>block()</expr_1>
    after()
}

fun before() {}
fun after() {}