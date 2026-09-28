// RUN_PIPELINE_TILL: CODEGEN
class Bar(name: () -> String) {
    val name = name()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionalType, primaryConstructor, propertyDeclaration */
