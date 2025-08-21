// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +AllowContractsOnPropertyAccessors

import kotlin.contracts.*

open class Test {
    open val property: Int?
        get() {
            <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (property != null) }
            return 1
        }
}

class Child: Test() {
    override val property: Int?
        get() {
            <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (property != null) }
            return 2
        }
}

class A

val A.property: Boolean
    get() {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (this@property == A())<!>
        }
        return true
    }

val A.property1: String
    get() {
        contract {
            <!ANNOTATION_IN_CONTRACT_ERROR!>@Suppress("SENSELESS_COMPARISON")<!>
            returns(true) implies (this@property1 != null)
        }
        return ""
    }

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contracts, equalityExpression, getter,
integerLiteral, lambdaLiteral, nullableType, override, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral,
thisExpression */
