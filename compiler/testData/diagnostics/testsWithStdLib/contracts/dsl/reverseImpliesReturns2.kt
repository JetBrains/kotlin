// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ConditionImpliesReturnsContracts, +DataFlowBasedExhaustiveness

import kotlin.contracts.*

fun acceptString(x: String) {}

// KT-79218
val String?.foo: String?
    get() {
        contract { (this@foo == null) implies returnsNotNull() }
        return if (this == null) "" else null
    }

fun test_transitive() {
    val x = null
    x.foo.length
}

// KT-79220
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

// KT-79220
fun String?.foo(): String? {
    contract { (this@foo == null) implies returnsNotNull() }
    return if (this == null) "" else null
}

fun usage(){
    null.foo().length
    acceptString(null.foo())
    val a = null.foo()
    a.length
}

// KT-79355
operator fun Any?.inc(): Int? {
    contract {
        returns() implies (this@inc != null)
        (this@inc != null) implies returnsNotNull()
    }
    return (this as Int) + 1
}

fun test_inc_dec(ix1: Int?) {
    var x1 = ix1
    x1++
    x1.toChar()
}

// KT-79277
fun decode(encoded: String?): String? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    return encoded
}

fun nestedUsage(x: String) {
    acceptString(decode(x))
}

fun directUsage(x: String) {
    decode(x).length
}

// KT-79271
sealed interface Variants {
    data object A : Variants
    data object B : Variants
    fun foo(){}
}

fun ensureA(v: Variants): Variants? {
    contract {
        (v is Variants.A) implies (returnsNotNull())
    }
    return v as Variants.A
}

fun foo(v: Variants.A): String {
    ensureA(v).foo()
    return when (ensureA(v)) {
        is Variants.B -> "B"
        is Variants.A -> "A"
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, asExpression, assignment, classDeclaration, contractConditionalEffect,
contracts, data, destructuringDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, getter,
ifExpression, incrementDecrementExpression, integerLiteral, interfaceDeclaration, isExpression, lambdaLiteral,
localProperty, nestedClass, nullableType, objectDeclaration, operator, primaryConstructor, propertyDeclaration,
propertyWithExtensionReceiver, sealed, smartcast, stringLiteral, thisExpression, typeParameter, whenExpression,
whenWithSubject */
