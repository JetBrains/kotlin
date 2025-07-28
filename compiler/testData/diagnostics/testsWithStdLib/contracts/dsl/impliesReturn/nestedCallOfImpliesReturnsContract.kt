// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.*

fun impliesReturn(encoded: String?): String? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    return encoded
}

fun returnNotNullImplies(x: Any?): String? {
    contract {
        returnsNotNull() implies (x is String)
    }
    return x as? String
}

fun nestedcall(x: String, y: Any) {
    impliesReturn(impliesReturn(x)).length
    impliesReturn(returnNotNullImplies(x)).length
    impliesReturn(returnNotNullImplies(y))<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contractImpliesReturnEffect, contracts, equalityExpression,
functionDeclaration, isExpression, lambdaLiteral, nullableType, smartcast */
