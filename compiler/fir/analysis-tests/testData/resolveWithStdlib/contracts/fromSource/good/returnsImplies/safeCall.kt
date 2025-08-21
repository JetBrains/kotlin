// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.RequiresOptIn
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun test1(x: String?): Int? {
    contract {
        returnsNotNull() implies (x != null)
    }

    return x?.length
}

@OptIn(ExperimentalContracts::class)
fun test2(x: String?): Int? {
    contract {
        returnsNotNull() implies (<!USELESS_IS_CHECK!>x is Boolean<!>)
    }

    return x?.length
}

/* GENERATED_FIR_TAGS: classReference, contractConditionalEffect, contracts, equalityExpression, functionDeclaration,
isExpression, lambdaLiteral, nullableType, safeCall */
