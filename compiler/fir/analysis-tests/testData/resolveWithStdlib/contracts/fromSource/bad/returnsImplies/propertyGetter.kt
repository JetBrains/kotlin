// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.RequiresOptIn
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun Any?.isNotNull(): Boolean {
    contract {
        returns(true) implies (this@isNotNull != null)
    }
    return this != null
}

@OptIn(ExperimentalContracts::class)
val Any?.isNotNull: Boolean
    get() {
        <!CONTRACT_NOT_ALLOWED!>contract<!> {
            returns(true) implies (this@isNotNull != null)
        }
        return this@isNotNull != null
    }

/* GENERATED_FIR_TAGS: classReference, contractConditionalEffect, contracts, equalityExpression,
funWithExtensionReceiver, functionDeclaration, getter, lambdaLiteral, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, thisExpression */
