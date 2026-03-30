// RUN_PIPELINE_TILL: BACKEND
fun foo(f : () -> Unit) {
    val x : Unit = f()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, localProperty, propertyDeclaration */
