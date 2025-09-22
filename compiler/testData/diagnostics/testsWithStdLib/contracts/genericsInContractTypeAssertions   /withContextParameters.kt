// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*
import kotlin.reflect.KClass

context(sb: StringBuilder)
fun <T : Any> ctxCheck(k: KClass<T>, v: Any?): Boolean {
    contract { returns(true) implies (v is T) }
    return k.isInstance(v)
}

fun ctxTest(x: Any?) {
    with(StringBuilder()) {
        if (ctxCheck(String::class, x)) {
            x.length
        }
    }
}

context(a: Any?)
inline fun <reified T> isA(): Boolean {
    contract { returns(true) implies (a is T) }
    return a is T
}

context(a: Any?)
fun test() {
    if (isA<String>()) {
        a.length
    }
}

/* GENERATED_FIR_TAGS: classReference, contractConditionalEffect, contracts, functionDeclaration,
functionDeclarationWithContext, ifExpression, inline, isExpression, lambdaLiteral, nullableType, reified, smartcast,
typeConstraint, typeParameter */
