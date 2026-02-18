// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.*

context(encoded: String?)
val decode: String?
    get() {
        contract {
            (encoded != null) implies (returnsNotNull())
        }
        if (encoded == null) return null
        return encoded + "a"
    }

context(a: (() -> String)?)
val withFunctionalContext: String?
    get() {
        contract {
            (a != null) implies (returnsNotNull())
        }
        return a?.invoke()
    }

context(a: T)
val <T> withTypeParam: String?
    get(){
        contract {
            (a != null) implies (returnsNotNull())
        }
        return null
    }

fun usage1(s: String?, a: (() -> String)?) {
    with(s) {
        if (s != null) decode<!UNSAFE_CALL!>.<!>length
        if (this != null) decode.length
    }

    with(a) {
        if (a != null) withFunctionalContext<!UNSAFE_CALL!>.<!>length
        if (this != null) withFunctionalContext.length
    }
}

context(s: String?, a: (() -> String)?)
fun usage2() {
    if (s != null) decode.length
    if (a != null) withFunctionalContext.length
}

context(a: T)
fun <T : Any> usage3() {
    with(a) {
        withTypeParam.length
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, contractImpliesReturnEffect, contracts, equalityExpression,
functionDeclaration, functionDeclarationWithContext, functionalType, getter, ifExpression, lambdaLiteral, nullableType,
propertyDeclaration, propertyDeclarationWithContext, safeCall, smartcast, stringLiteral, thisExpression, typeConstraint,
typeParameter */
