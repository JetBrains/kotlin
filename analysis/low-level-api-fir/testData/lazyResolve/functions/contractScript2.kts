// RESOLVE_SCRIPT
import kotlin.contracts.InvocationKind

inline fun foo(block: () -> Unit) {
    kotlin.contracts.contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    block()
}
