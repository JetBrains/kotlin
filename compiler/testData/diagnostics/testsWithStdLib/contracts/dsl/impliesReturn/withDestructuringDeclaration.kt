// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// LANGUAGE: +AllowContractsOnSomeOperators, +ConditionImpliesReturnsContracts
// ISSUE: KT-79220
import kotlin.contracts.*

class PairList<T>(val items: List<T>)

operator fun <T> PairList<T>?.component1(): T {
    contract {
        (this@component1!= null) implies (returnsNotNull())
    }
    @Suppress("UNCHECKED_CAST")
    return true as T
}

operator fun <T> PairList<T>?.component2(): T {
    @Suppress("UNCHECKED_CAST")
    return true as T
}

fun testDestructuring(pair: PairList<String?>) {
    val (first, second) = pair
    first.length
    pair.component1().length
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, contractImpliesReturnEffect, contracts, destructuringDeclaration,
equalityExpression, funWithExtensionReceiver, functionDeclaration, lambdaLiteral, localProperty, nullableType, operator,
primaryConstructor, propertyDeclaration, smartcast, stringLiteral, thisExpression, typeParameter */
