// RUN_PIPELINE_TILL: FRONTEND
private var x = object {}

fun test() {
    x <!ASSIGNMENT_TYPE_MISMATCH!>=<!> object {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, functionDeclaration, propertyDeclaration */
