// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUES: KT-79221
import kotlin.contracts.*

context(value: Any?)
val <T> T.checkT: Boolean
    get() {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (value is T)<!>     //false negative ERROR_IN_CONTRACT_DESCRIPTION - KT-79221
        }
        return true
    }

context(value: T)
val <T> Any.checkTFromContext: Boolean
    get() {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (this@checkTFromContext is T)<!>     //false negative ERROR_IN_CONTRACT_DESCRIPTION - KT-79221
        }
        return true
    }

fun usageErased(s: Any?) {
    with(s) {
        if ("".checkT) {
            this.<!UNRESOLVED_REFERENCE!>length<!>
        }
    }
}

context(value: Any?)
fun usageErased2() {
    if ("".checkT) {
        value.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun usageErased3(s: String, x: Any) {
    with(s) {
        x.checkTFromContext
        x.<!UNRESOLVED_REFERENCE!>length<!>
        0
    }
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, functionDeclaration, functionDeclarationWithContext, getter,
ifExpression, integerLiteral, isExpression, lambdaLiteral, nullableType, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral, thisExpression, typeParameter */
