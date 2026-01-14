// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

context(noinline block: () -> R)
<!NOTHING_TO_INLINE!>inline<!> fun <R> myRunOnce(): R {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }<!>
    return block()
}

fun usage() {
    val x: String
    with({
             <!CAPTURED_VAL_INITIALIZATION!>x<!> = ""
         }) {
        myRunOnce()
    }
}

/* GENERATED_FIR_TAGS: assignment, contractCallsEffect, contracts, functionDeclaration, functionDeclarationWithContext,
functionalType, inline, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter */
