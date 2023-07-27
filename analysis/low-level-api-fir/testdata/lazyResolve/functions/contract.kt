import kotlin.contracts.InvocationKind

inline fun f<caret>oo(block: () -> Unit) {
    kotlin.contracts.contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    block()
}
