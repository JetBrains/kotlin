// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-90662
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

open class Base<T> {
    fun isString(x: Any?): Boolean {
        contract { returns(true) implies (x is String) }
        return x is String
    }
}

open class Mid<U> : Base<U>()

class Leaf : Mid<Int>()

fun viaBase(base: Base<Int>, a: Any?): Int = if (base.isString(a)) a.length else 0

fun viaMid(mid: Mid<Int>, a: Any?): Int = if (mid.isString(a)) a.length else 0

fun <T> viaMidWithTypeParameter(mid: Mid<T>, a: Any?): Int = if (mid.isString(a)) a.length else 0

fun viaLeaf(leaf: Leaf, a: Any?): Int = if (leaf.isString(a)) a.length else 0

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contracts, functionDeclaration, ifExpression,
integerLiteral, isExpression, lambdaLiteral, nullableType, smartcast, typeParameter */
