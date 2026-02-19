// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.*

context(a: Int?)
operator fun String?.invoke(): Int? {
    contract {
        (a != null) implies (returnsNotNull())
    }
    return null
}

fun testInvoke(x: Int) {
    with(x) {
        ""().inc()
    }
}

/* GENERATED_FIR_TAGS: contractImpliesReturnEffect, contracts, equalityExpression, funWithExtensionReceiver,
functionDeclaration, functionDeclarationWithContext, lambdaLiteral, nullableType, operator, smartcast, stringLiteral */
