import kotlin.contracts.contract
import kotlin.contracts.InvocationKind

inline fun foo(block: () -> Unit) {
    <expr>contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }</expr>

    block()
}
