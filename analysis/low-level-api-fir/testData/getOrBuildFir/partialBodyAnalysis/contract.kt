import kotlin.contracts.*

inline fun foo(block: () -> Unit) {
    contract {
        <expr>callsInPlace(block, InvocationKind.EXACTLY_ONCE)</expr>
    }

    before()
    <expr_1>block()</expr_1>
    after()
}

fun before() {}
fun after() {}