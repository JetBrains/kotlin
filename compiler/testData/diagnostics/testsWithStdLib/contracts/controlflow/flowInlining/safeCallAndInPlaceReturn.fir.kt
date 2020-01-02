// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

inline fun <T> Any?.myRun(block: () -> T): T {
    contract {
        <!INAPPLICABLE_CANDIDATE!>callsInPlace<!>(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun bad(): String {
    val x: String? = null

    x?.myRun { return "" }
}

fun ok(): String {
    val x: String? = null

    x?.run { return "non-null" } ?: return "null"
}