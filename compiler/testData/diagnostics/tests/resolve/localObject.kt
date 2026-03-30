// RUN_PIPELINE_TILL: FRONTEND
fun foo(): Any {
    <!LOCAL_OBJECT_NOT_ALLOWED!>object Bar<!>
    return Bar
}

/* GENERATED_FIR_TAGS: functionDeclaration, localClass, objectDeclaration */
