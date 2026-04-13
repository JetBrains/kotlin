// RUN_PIPELINE_TILL: FRONTEND
class Test {
    private var x = object {};
    init {
        x <!ASSIGNMENT_TYPE_MISMATCH!>=<!> object {}
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, classDeclaration, init, propertyDeclaration */
