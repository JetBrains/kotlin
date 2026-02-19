// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

fun <T> T.alsoIf(condition: Boolean, block: (T) -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        <!ERROR_IN_CONTRACT_DESCRIPTION!>condition holdsIn block<!>
    }
    if (condition) block(this)
    return this
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression, lambdaLiteral,
nullableType, thisExpression, typeParameter */
