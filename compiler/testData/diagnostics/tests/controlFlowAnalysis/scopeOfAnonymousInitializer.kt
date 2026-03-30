// RUN_PIPELINE_TILL: BACKEND
class AnonymousInitializers(var a: String) {
    init {
        a = "s"
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, primaryConstructor, propertyDeclaration, stringLiteral */
