// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.contract

open class OpenHost {
    open var Int?.b: String
        get() = ""
        set(value) {
            <!CONTRACT_NOT_ALLOWED!>contract<!> {
                returns() implies (this@b != null)
            }
        }

    open var b0: String
        get() = ""
        set(value) {
            <!CONTRACT_NOT_ALLOWED!>contract<!> {
                returns()
            }
        }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contractReturnsEffect, contracts, equalityExpression,
getter, lambdaLiteral, nullableType, propertyDeclaration, propertyWithExtensionReceiver, setter, stringLiteral,
thisExpression */
