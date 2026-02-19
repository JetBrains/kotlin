// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.*

sealed interface Variants {
    data object A : Variants
    data object B : Variants
    fun foo(){}
}
context(v: Variants)
fun ensureA(): Variants? {
    contract {
        (v is Variants.A) implies (returnsNotNull())
    }
    return null
}

context(v: Variants.A)
fun foo(): String {
    return when (ensureA()) { 
        is Variants.B -> "B"
        is Variants.A -> "A"
    }
}

/* GENERATED_FIR_TAGS: contractImpliesReturnEffect, contracts, data, functionDeclaration, functionDeclarationWithContext,
interfaceDeclaration, isExpression, lambdaLiteral, nestedClass, nullableType, objectDeclaration, sealed, smartcast,
stringLiteral, whenExpression, whenWithSubject */
