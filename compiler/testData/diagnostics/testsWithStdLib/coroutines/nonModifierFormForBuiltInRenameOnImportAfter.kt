// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// SKIP_TXT
// LANGUAGE: +ParseLambdaWithSuspendModifier

import kotlin.suspend as suspendLambda

fun bar() {
    suspend {
        println()
    }

    kotlin.<!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!> {

    }

    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspendLambda<!> {
        println()
    }

    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspendLambda<!>() {
        println()
    }

    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspendLambda<!>({ println() })

    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspendLambda<!><Unit> {
        println()
    }

    val w: (suspend () -> Int) -> Any? = ::<!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspendLambda<!>
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, suspend */
