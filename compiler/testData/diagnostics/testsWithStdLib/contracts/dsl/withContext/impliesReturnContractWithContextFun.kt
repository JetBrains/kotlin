// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.*

context(encoded: String?)
fun decode(): String? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    if (encoded == null) return null
    return encoded + "a"
}

context(_: String?)
fun withUnnamedContext(): String? {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>(contextOf<String?>() != null) implies (returnsNotNull())<!>
    }
    return ""
}

context(a: (()->String)?)
fun withFunctionalContext(): String? {
    contract {
        (a != null) implies (returnsNotNull())
    }
    return a?.invoke()
}

context(a: T)
fun <T> testTypeParam(): String? {
    contract {
        (a != null) implies (returnsNotNull())
    }
    return null
}

fun usage1(s: String?, a: (() -> String)?) {
    with(s) {
        if (s != null) decode()<!UNSAFE_CALL!>.<!>length
        if (this != null) decode().length
        if (s != null) testTypeParam()<!UNSAFE_CALL!>.<!>length
        if (this != null) testTypeParam().length
    }

    with(a) {
        if(a != null) withFunctionalContext()<!UNSAFE_CALL!>.<!>length
        if(this != null) withFunctionalContext().length
    }
}

context(s: String?, a: (()-> String)?)
fun usage2() {
    if (s != null) decode().length
    if (a != null) withFunctionalContext().length
    if (s != null) testTypeParam<String>().length
}

/* GENERATED_FIR_TAGS: additiveExpression, contractImpliesReturnEffect, contracts, equalityExpression,
functionDeclaration, functionDeclarationWithContext, functionalType, ifExpression, lambdaLiteral, nullableType, safeCall,
smartcast, stringLiteral, thisExpression, typeParameter */
