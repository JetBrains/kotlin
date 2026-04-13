// MODULE: original
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind

@OptIn(ExperimentalContracts::class)
inline fun foo(block: () -> Unit) {
    kotlin.contracts.contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    block()
}

// MODULE: copy
// COMPILATION_ERRORS
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind

@OptIn(ExperimentalContracts::class)
inline fun foo(block: () -> Unit)