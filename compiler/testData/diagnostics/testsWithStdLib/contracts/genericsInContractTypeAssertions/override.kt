// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*
import kotlin.reflect.KClass

interface Checker {
    fun <T : Any> check(k: KClass<T>, v: Any?): Boolean
}

class Impl : Checker {
    override fun <T : Any> check(k: KClass<T>, v: Any?): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(true) implies (v is T) }
        return k.isInstance(v)
    }
}

fun test(checker: Checker, x: Any) {
    if (checker.check(String::class, x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, contractConditionalEffect, contracts, functionDeclaration,
ifExpression, interfaceDeclaration, isExpression, lambdaLiteral, nullableType, override, typeConstraint, typeParameter */
