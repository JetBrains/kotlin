// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

class A { fun foo(s: String) {} }

class Host {
    context(a: A?)
    val Unit.hasA_M: Boolean
        get() {
            contract { returns(true) implies (a != null) }
            return a != null
        }
}

context(a: A?)
fun usageMember(h: Host) {
    with(h) {
        if (Unit.hasA_M) {
            a.foo("")
        } else {
            a<!UNSAFE_CALL!>.<!>foo("")
        }
    }
}

context(a: A?)
val hasATop: Boolean
    get() {
        contract { returns(true) implies (a != null) }
        return a != null
    }

context(a: A?)
fun usageTop() {
    if (hasATop) {
        a.foo("")
    } else {
        a<!UNSAFE_CALL!>.<!>foo("")
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, contractConditionalEffect, contracts, dnnType,
equalityExpression, functionDeclaration, functionDeclarationWithContext, getter, ifExpression, lambdaLiteral,
nullableType, primaryConstructor, propertyDeclaration, propertyDeclarationWithContext, propertyWithExtensionReceiver,
smartcast, stringLiteral, thisExpression, typeParameter */
