// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.*

fun foo(a: Int?): String? {
    contract {
        (a != null) implies (returnsNotNull())
    }
    return ""
}

fun testChangingClosure(a: Int) {
    var x = a
    {
        x = 4
    }
    foo(x)<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: assignment, contractImpliesReturnEffect, contracts, equalityExpression, functionDeclaration,
integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral */
