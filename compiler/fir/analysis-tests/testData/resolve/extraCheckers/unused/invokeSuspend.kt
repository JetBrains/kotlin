// RUN_PIPELINE_TILL: BACKEND
suspend fun foo(action: suspend () -> Unit) {
    val x = action

    x()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, localProperty, propertyDeclaration, suspend */
