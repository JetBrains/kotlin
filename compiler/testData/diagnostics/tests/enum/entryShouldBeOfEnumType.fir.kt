// RUN_PIPELINE_TILL: BACKEND
enum class E {
    E1,
    E2
}

fun foo() {
    var e = E.E1
    e = E.E2
}

/* GENERATED_FIR_TAGS: assignment, enumDeclaration, enumEntry, functionDeclaration, localProperty, propertyDeclaration */
