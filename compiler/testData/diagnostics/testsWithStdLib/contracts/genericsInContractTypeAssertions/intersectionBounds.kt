// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

interface A { fun a(): Int }
interface B { fun b(): String }

class AB : A, B {
    override fun a() = 42
    override fun b() = "ok"
}

fun <T> isAB(v: Any?): Boolean where T : A, T : B {
    contract { returns(true) implies (v is T) }
    return v is AB
}

fun usageB(x: Any) {
    if (isAB<AB>(x)) {
        x.a()
        x.b()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contracts, functionDeclaration, ifExpression,
integerLiteral, interfaceDeclaration, isExpression, lambdaLiteral, nullableType, override, smartcast, stringLiteral,
typeConstraint, typeParameter */
