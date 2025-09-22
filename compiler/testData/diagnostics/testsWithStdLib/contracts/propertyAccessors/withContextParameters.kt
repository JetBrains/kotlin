// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

class A { fun foo(s: String) {} }

context(a: A?)
val hasA: Boolean
    get() {
        contract { returns(true) implies (a != null) }
        return a != null
    }

context(a: A?)
fun usage() {
    if (hasA) {
        a.foo("")
    } else {
        a<!UNSAFE_CALL!>.<!>foo("")
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contracts, dnnType, equalityExpression,
functionDeclaration, functionDeclarationWithContext, getter, ifExpression, lambdaLiteral, nullableType,
primaryConstructor, propertyDeclaration, propertyDeclarationWithContext, propertyWithExtensionReceiver, smartcast,
stringLiteral, thisExpression, typeParameter */
