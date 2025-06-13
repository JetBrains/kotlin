// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DUMP_CFG: LEVELS

lateinit var s: String

fun foo() {
    s = "Hello"
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, lateinit, localProperty, propertyDeclaration, stringLiteral */
