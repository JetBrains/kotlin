// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.RequiresOptIn
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun testAlwaysNotNull(x: String?): Any? {
    contract {
        returnsNotNull() implies (x is String && <!SENSELESS_COMPARISON!>x != null<!>)
    }

    return x
}

@OptIn(ExperimentalContracts::class)
fun testAlwaysAny(x: String?): Any? {
    contract {
        returnsNotNull() implies (<!USELESS_IS_CHECK!>x is String?<!> || <!USELESS_IS_CHECK!>x is Any?<!>)
    }

    return x
}

/* GENERATED_FIR_TAGS: andExpression, classReference, contractConditionalEffect, contracts, disjunctionExpression,
equalityExpression, functionDeclaration, isExpression, lambdaLiteral, nullableType, smartcast */
