// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUES: KT-79025
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

context(noinline block: () -> R) inline val <R> myRunOnce: R
    get() {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)            //should be forbidden KT-79025
        }
        return block()
    }

fun usage() {
    val x: String
    with({
             <!CAPTURED_VAL_INITIALIZATION!>x<!> = ""
         }){
        myRunOnce
    }
}

/* GENERATED_FIR_TAGS: assignment, contractCallsEffect, contracts, functionDeclaration, functionalType, getter,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, propertyDeclarationWithContext, stringLiteral,
typeParameter */
