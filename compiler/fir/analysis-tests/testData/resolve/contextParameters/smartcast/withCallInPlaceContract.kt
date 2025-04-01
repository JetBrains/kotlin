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
    <!ERROR_IN_CONTRACT_DESCRIPTION("callsInPlace contract cannot be applied to context parameter because context arguments can never be lambdas.")!>contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }<!>
    return block()
}

@ExperimentalContracts
fun test() {
    var x: Any = materialize()
    if (x !is String) return
    // Lambda can never be passed as context argument, therefore invocation kinds are meaningless for context parameters.
    // Note that the context argument to the call myRun() is not the lambda, but the extension receiver of the lambda argument to `with`.
    with({
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x = 10
    }) {
        myRun()
    }
}
