// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: 80492
// LANGUAGE: +CollectionLiterals

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
class MyList<T> {
    companion object {
        operator fun <T> of(vararg vs: T): MyList<T & Any> {
            <!CONTRACT_NOT_ALLOWED!>contract<!> {
                returns() implies (vs is Array<out T & Any>)
            }
            TODO()
        }

        operator fun <T> of(v: T): MyList<T & Any> {
            <!CONTRACT_NOT_ALLOWED!>contract<!> {
                returns() implies (v is (T & Any))
            }
            TODO()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, contractConditionalEffect, contracts, dnnType,
functionDeclaration, isExpression, lambdaLiteral, nullableType, objectDeclaration, operator, outProjection,
typeParameter, vararg */
