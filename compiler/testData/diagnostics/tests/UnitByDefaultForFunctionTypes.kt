// RUN_PIPELINE_TILL: CODEGEN
fun foo(f : () -> Unit) {
    val x : Unit = f()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, localProperty, propertyDeclaration */
