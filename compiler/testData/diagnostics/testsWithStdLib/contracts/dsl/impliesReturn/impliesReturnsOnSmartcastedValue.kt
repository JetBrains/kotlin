// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.*

fun foo(a: Int?): String? {
    contract {
        (a != null) implies (returnsNotNull())
    }
    return ""
}

fun test(bar: Int?) {
    val nonNull = bar != null
    if (nonNull) {
        foo(bar).length
    }
}

fun test2(x: Any) {
    when {
        x is Int -> foo(x).length
    }
}

/* GENERATED_FIR_TAGS: contractImpliesReturnEffect, contracts, equalityExpression, functionDeclaration, ifExpression,
isExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral, whenExpression */
