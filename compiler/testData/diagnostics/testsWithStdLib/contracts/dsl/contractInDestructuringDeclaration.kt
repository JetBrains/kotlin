// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +AllowContractsOnSomeOperators +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
import kotlin.contracts.*

class PairList<T>(val items: List<T>)

operator fun <T> PairList<T>?.component1(): T {
    contract { returns() implies (this@component1 != null) }
    if (this == null) throw IllegalStateException()
    return items[0]
}

operator fun <T> PairList<T>?.component2(): T {
    if (this == null) throw IllegalStateException()
    return items[0]
}

operator fun  String.component1(): String {
    return this
}

operator fun  String.component2(): String {
    return this
}

operator fun <T> PairList<T>?.iterator(): Iterator<T> {
    contract { returns() implies (this@iterator != null) }
    if (this == null) throw IllegalStateException()
    return items.iterator()
}

fun destructuringTest(pair: PairList<String>?) {
    val [first, second] = pair
    pair.items.size
}

fun iteratorTest(pair: PairList<String>?) {
    for ([a,b] in pair) {
        pair.items.size
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contracts, destructuringDeclaration,
equalityExpression, forLoop, funWithExtensionReceiver, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral,
localProperty, nullableType, operator, primaryConstructor, propertyDeclaration, smartcast, thisExpression, typeParameter */
