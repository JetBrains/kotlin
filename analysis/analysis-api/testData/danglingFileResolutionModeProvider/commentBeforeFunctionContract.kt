// MODULE: original
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind

@OptIn(ExperimentalContracts::class)
inline fun foo(block: () -> Unit) {
    kotlin.contracts.contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    block()
}

// MODULE: copy
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind

@OptIn(ExperimentalContracts::class)
inline fun foo(block: () -> Unit) {
    // SOMETHING
    kotlin.contracts.contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    block()
}