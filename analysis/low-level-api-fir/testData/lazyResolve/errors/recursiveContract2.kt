@file:OptIn(kotlin.contracts.ExperimentalContracts::class)
import kotlin.contracts.*

inline fun nonLocalCase(block: () -> Unit = {}): Boolean {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    block()
    return true
}

fun cas<caret>e_4(): Boolean? {
    contract { returns(null) implies case_1() }
    return true
}
