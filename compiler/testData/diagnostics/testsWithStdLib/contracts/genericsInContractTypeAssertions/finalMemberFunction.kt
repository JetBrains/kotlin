// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*
import kotlin.reflect.KClass

open class Base {
    final fun <T : Any> check(k: KClass<T>, v: Any?): Boolean {
        contract { returns(true) implies (v is T) }
        return k.isInstance(v)
    }
}

class Impl : Base()

fun finalTest(base: Base, x: Any) {
    if (base.check(String::class, x)) {
        x.length
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, contractConditionalEffect, contracts, functionDeclaration,
ifExpression, isExpression, lambdaLiteral, nullableType, smartcast, typeConstraint, typeParameter */
