// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class AnonymousInitializers(var a: String) {
    init {
        a = "s"
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, primaryConstructor, propertyDeclaration, stringLiteral */
