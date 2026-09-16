// RUN_PIPELINE_TILL: CODEGEN
fun test() {
    class Local

    val l = Local()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, localProperty, propertyDeclaration */
