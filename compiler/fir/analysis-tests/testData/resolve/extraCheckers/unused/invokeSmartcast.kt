// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78664

fun func() {}

fun bar() {
    val f: () -> Unit
    f = ::func
    f()
}

/* GENERATED_FIR_TAGS: assignment, callableReference, functionDeclaration, functionalType, localProperty,
propertyDeclaration, smartcast */
