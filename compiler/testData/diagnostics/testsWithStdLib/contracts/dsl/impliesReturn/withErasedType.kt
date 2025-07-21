// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ConditionImpliesReturnsContracts, +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.*

fun <T> testErasedType(a: Any?, b: T): String? {
    contract {
        (a is T) implies (returnsNotNull())
    }
    return ""
}

fun usage(a: String, b: String) {
    testErasedType(a, b).length
}

/* GENERATED_FIR_TAGS: contractImpliesReturnEffect, contracts, functionDeclaration, isExpression, lambdaLiteral,
nullableType, stringLiteral, typeParameter */
