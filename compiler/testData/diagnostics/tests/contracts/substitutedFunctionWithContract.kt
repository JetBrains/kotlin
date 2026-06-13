// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-85661
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

val outcome: Outcome<String, Long> = null!!

fun foo() {
    if (outcome.isIncomplete()) {

    }
}

class Outcome<out I, out T : Any> private constructor(private val value: Any) {
    @OptIn(ExperimentalContracts::class)
    fun isIncomplete(): Boolean {
        contract {
            returns(false) implies (this@Outcome is Outcome<Nothing, T>)
            returns(true) implies (this@Outcome is Outcome<I, Nothing>)
        }
        return value is Incomplete
    }

    private data class Incomplete(val incompleteValue: Any?)
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, classReference, contractConditionalEffect, contracts, data,
functionDeclaration, ifExpression, isExpression, lambdaLiteral, nestedClass, nullableType, out, primaryConstructor,
propertyDeclaration, thisExpression, typeConstraint, typeParameter */
