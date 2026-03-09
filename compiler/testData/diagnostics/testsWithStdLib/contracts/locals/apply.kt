// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +ImprovedAliasTracking

import kotlin.contracts.*

class Lateinit<R : Any> {
    lateinit var value: R
}

@OptIn(ExperimentalContracts::class)
public inline fun <R : Any> build(crossinline builder: Lateinit<R>.() -> Unit): R {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return Lateinit<R>().apply(builder).value
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, contractCallsEffect, contracts, crossinline,
functionDeclaration, functionalType, inline, lambdaLiteral, lateinit, propertyDeclaration, typeConstraint, typeParameter,
typeWithExtension */
