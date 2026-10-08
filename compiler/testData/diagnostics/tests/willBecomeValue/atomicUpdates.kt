// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

import kotlin.concurrent.atomics.*

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

fun test(ref: AtomicReference<Wrapper>, array: AtomicArray<Wrapper>, notWrapper: AtomicReference<String>) {
    ref.update { it }
    ref.fetchAndUpdate { it }
    ref.updateAndFetch { it }
    array.updateAt(0) { it }
    array.fetchAndUpdateAt(0) { it }
    array.updateAndFetchAt(0) { it }
    notWrapper.update { it }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, integerLiteral,
isExpression, lambdaLiteral, nullableType, operator, override, primaryConstructor, propertyDeclaration, smartcast,
stringLiteral */
