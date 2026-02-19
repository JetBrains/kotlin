// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

open class Test {
    fun foo() {}
}

fun <T : Test?> checkT(value: Any?, b: T & Any): Boolean {
    contract {
        returns(true) implies (value is T)
    }
    return true
}

fun usage(x: Any?) {
    if (checkT(x, Test())) {
        x.foo()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contracts, dnnType, functionDeclaration,
ifExpression, isExpression, lambdaLiteral, nullableType, smartcast, typeConstraint, typeParameter */
