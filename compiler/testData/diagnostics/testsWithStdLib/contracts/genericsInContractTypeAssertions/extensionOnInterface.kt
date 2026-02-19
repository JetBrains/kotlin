// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*
import kotlin.reflect.KClass

interface I

fun <T : Any> I.check(kClass: KClass<T>, value: Any?): Boolean {
    contract { returns(true) implies (value is T) }
    return kClass.isInstance(value)
}

fun usage(a: I, x: Any) {
    if (a.check(String::class, x)) {
        x.length
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, contractConditionalEffect, contracts, funWithExtensionReceiver,
functionDeclaration, ifExpression, interfaceDeclaration, isExpression, lambdaLiteral, nullableType, override, smartcast,
typeConstraint, typeParameter */
