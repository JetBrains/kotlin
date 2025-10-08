// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object A {
    fun funA(): String {
        return "A.funA body"
    }

    inline fun funB(): String {
        return "A.funB body"
    }

    @OptIn(ExperimentalContracts::class)
    fun isNotNull(value: Any?): Boolean {
        contract {
            returns(true) implies (value != null)
        }
        return value != null
    }

    private fun funC(): String {
        return "A.funC body"
    }
}

/* GENERATED_FIR_TAGS: classReference, contractConditionalEffect, contracts, functionDeclaration, inline, nullableType,
objectDeclaration, stringLiteral */
