// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
fun test() {
    var x = object {}
    x <!ASSIGNMENT_TYPE_MISMATCH!>=<!> object {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, functionDeclaration, localProperty, propertyDeclaration */
