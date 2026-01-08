// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.*

context(condition: Boolean, block: () -> Unit, x: String?)
fun foo() {
    fun blockAndConditionFromContext(): Unit? {
        <!CONTRACT_NOT_ALLOWED!>contract<!> {
            condition holdsIn block
        }
        return null
    }

    with(x is String) {
        with({ x<!UNSAFE_CALL!>.<!>length }) {
            blockAndConditionFromContext()
        }
    }
}

/* GENERATED_FIR_TAGS: contractHoldsInEffect, contracts, functionDeclaration, functionDeclarationWithContext,
functionalType, isExpression, lambdaLiteral, localFunction, nullableType */
