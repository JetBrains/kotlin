// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

interface I {
    var Int?.propA1 : String
    var propA2: String
}

open class A : I {
    override var Int?.propA1 : String
        get() = ""
        set(value) {
            <!CONTRACT_NOT_ALLOWED!>contract<!> {
                returns() implies (this@propA1 != null)
            }
        }

    override var propA2: String
        get() = ""
        set(value) {
            <!CONTRACT_NOT_ALLOWED!>contract<!> {
                returns()
            }
        }
}

@Suppress("SENSELESS_COMPARISON")
open class B : A() {
    var Int?.propB1 : String
        get() = ""
        set(value) {
            contract {
                <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (this@propB1 != null && 1.propA1 != null && this@B.propA2 != null)<!>
            }
        }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, contractConditionalEffect, contractReturnsEffect, contracts,
equalityExpression, getter, integerLiteral, interfaceDeclaration, lambdaLiteral, nullableType, override,
propertyDeclaration, propertyWithExtensionReceiver, setter, stringLiteral, thisExpression */
