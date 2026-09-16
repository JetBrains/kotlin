// RUN_PIPELINE_TILL: CODEGEN
class AnonymousInitializers(var a: String) {
    init {
        a = "s"
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, primaryConstructor, propertyDeclaration, stringLiteral */
