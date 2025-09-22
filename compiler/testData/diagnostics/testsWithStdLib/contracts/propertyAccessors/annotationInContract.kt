// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

class A

val A.property: String
    get() {
        contract {
            <!ANNOTATION_IN_CONTRACT_ERROR!>@Suppress("SENSELESS_COMPARISON")<!>
            returns(true) implies (this@property != null)
        }
        return ""
    }

val property: String
    get() {
        contract {
            <!ANNOTATION_IN_CONTRACT_ERROR!>@Suppress("SENSELESS_COMPARISON")<!>
            returns(true)
        }
        return ""
    }

class B {
    val property: String
        get() {
            contract {
                <!ANNOTATION_IN_CONTRACT_ERROR!>@Suppress("SENSELESS_COMPARISON")<!>
                returns(true)
            }
            return ""
        }
}

object C {
    val A.property: String
        get() {
            contract {
                <!ANNOTATION_IN_CONTRACT_ERROR!>@Suppress("SENSELESS_COMPARISON")<!>
                returns(true) implies (this@property != null)
            }
            return ""
        }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contractReturnsEffect, contracts, equalityExpression,
getter, lambdaLiteral, objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral,
thisExpression */
