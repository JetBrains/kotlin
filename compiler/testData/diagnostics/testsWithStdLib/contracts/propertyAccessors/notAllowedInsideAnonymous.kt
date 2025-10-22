// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

val Boolean.case: Int
    get() = fun(): Int {
        <!CONTRACT_NOT_ALLOWED!>contract<!> {
            returns() implies (this@case)
        }
        return 1
    }()

class Host {
    val Boolean.case2: Int
        get() = fun(): Int {
            <!CONTRACT_NOT_ALLOWED!>contract<!> {
                returns() implies (this@case2)
            }
            return 1
        }()
}

val case3: Int
    get() = fun(): Int {
        <!CONTRACT_NOT_ALLOWED!>contract<!> {
            returns()
        }
        return 1
    }()

/* GENERATED_FIR_TAGS: anonymousFunction, classDeclaration, contractConditionalEffect, contractReturnsEffect, contracts,
getter, integerLiteral, lambdaLiteral, propertyDeclaration, propertyWithExtensionReceiver, thisExpression */
