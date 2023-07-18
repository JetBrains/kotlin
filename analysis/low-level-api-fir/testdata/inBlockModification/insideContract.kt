import kotlin.contracts.InvocationKind

inline fun foo(block: () -> Unit) {
    kotlin.contracts.contract {
        <expr>callsInPlace</expr>(block, InvocationKind.EXACTLY_ONCE)
    }

    block()
}
