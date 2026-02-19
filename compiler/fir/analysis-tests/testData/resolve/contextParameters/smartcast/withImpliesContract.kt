// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74572
// LANGUAGE: +ContextParameters
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

context(a: String?)
@ExperimentalContracts
fun validate() {
    contract {
        returns() implies (a!= null)
    }
}

context(a: String?)
@ExperimentalContracts
fun process() {
    validate()
    a.length
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, equalityExpression, functionDeclaration,
functionDeclarationWithContext, lambdaLiteral, nullableType, smartcast */
