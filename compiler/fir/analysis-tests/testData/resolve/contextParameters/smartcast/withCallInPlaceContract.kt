// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74572
// LANGUAGE: +ContextParameters
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.contracts.InvocationKind

fun <R> materialize(): R = null!!

context(block: () -> R)
@ExperimentalContracts
fun <R> myRun(): R {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!>
    }
    return block()
}

@ExperimentalContracts
fun test() {
    var x: Any = materialize()
    if (x !is String) return
    with({
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x = 10
    }) {
        myRun()
    }
}