// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +LateinitLocalVariables

fun test() {
    lateinit var s: String
    s = ""
    s.length
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, lateinit, localProperty, propertyDeclaration, stringLiteral */
