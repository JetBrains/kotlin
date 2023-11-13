import kotlin.contracts.InvocationKind

inline fun foo(block: () -> Unit) {
    <expr>kotlin.contracts.contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }</expr>

    block()
}
