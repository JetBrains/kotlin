// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun test1Warning(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
}

fun test1Suppress(block: () -> Unit) {
    @Suppress("WRONG_INVOCATION_KIND")
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
}

fun test2Warning(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
}

fun test2Suppress(block: () -> Unit) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>@Suppress("WRONG_INVOCATION_KIND")
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!>
    }
}
