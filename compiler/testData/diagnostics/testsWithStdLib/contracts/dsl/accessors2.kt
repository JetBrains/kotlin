// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ConditionImpliesReturnsContracts

import kotlin.contracts.*

/*
// KT-79218
val String?.foo: String?
    get() {
        contract { (this@foo == null) implies returnsNotNull() }
        return if (this == null) "" else null
    }

fun test_transitive() {
    val x = null
    x.foo.length // [SMARTCAST_IMPOSSIBLE] Smart cast to 'String' is impossible, because 'foo' is a property that has an open or custom getter.
}
*/

/*
// KT-79277
fun decode(encoded: String?): String? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    return encoded
}

fun nestedUsage(x: String) {
    acceptString(decode(x))// [ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is 'Boolean?', but 'Boolean' was expected.
}

fun directUsage(x: String) {
    decode(x).length
}

*/
fun acceptString(x: String) {}

/*
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

// KT-79220
class PairList<T>(val items: List<T>)

operator fun <T> PairList<T>?.component1(): T {
    contract {
        (this@component1!= null) implies (returnsNotNull())
    }
    return true as T
}

operator fun <T> PairList<T>?.component2(): T {
    return true as T
}

fun testDestructuring(pair: PairList<String?>) {
    val (first, second) = pair
    first.length                        //UNSAFE_CALL
    pair.component1().length            //OK
}
*/

fun String?.foo(): String? {
    contract { (this@foo == null) implies returnsNotNull() }
    return if (this == null) "" else null
}

fun usage(){
//    null.foo().length
//    acceptString(null.foo())
    val a = null.foo()
    a.length // [UNSAFE_CALL] Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'String?'.
}

/* GENERATED_FIR_TAGS: callableReference, contractConditionalEffect, contracts, equalityExpression, funInterface,
funWithExtensionReceiver, functionDeclaration, getter, ifExpression, interfaceDeclaration, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, propertyWithExtensionReceiver, smartcast, stringLiteral, thisExpression */
