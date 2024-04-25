// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

inline fun <T> Any?.myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

inline fun <T> directRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun bad(): String {
    val x: String? = null

    x?.myRun { return "" }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun ok(): String {
    val x: String? = null

    x?.run { return "non-null" } ?: return "null"
}

fun ok2(): String {
    directRun {
        return "nonNull"
    }
}

fun ok3(arg: Any?): String {
    arg?.myRun {
        return "nonNull"
    } ?: error("null")
}
