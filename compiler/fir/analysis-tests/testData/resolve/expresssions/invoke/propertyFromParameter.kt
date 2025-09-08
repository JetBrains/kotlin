// RUN_PIPELINE_TILL: BACKEND
class Bar(name: () -> String) {
    val name = name()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionalType, primaryConstructor, propertyDeclaration */
