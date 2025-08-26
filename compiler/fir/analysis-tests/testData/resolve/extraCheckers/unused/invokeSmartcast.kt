// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78664

fun func() {}

fun bar() {
    val <!VARIABLE_NEVER_READ!>f<!>: () -> Unit
    <!ASSIGNED_VALUE_IS_NEVER_READ!>f<!> = ::func
    f()
}

/* GENERATED_FIR_TAGS: assignment, callableReference, functionDeclaration, functionalType, localProperty,
propertyDeclaration, smartcast */
