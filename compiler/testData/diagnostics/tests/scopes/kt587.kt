// RUN_PIPELINE_TILL: BACKEND
// KT-587 Unresolved reference

class Main {
    companion object {
        class States() {
            companion object {
                public val N: States = States() // : States unresolved
            }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, nestedClass, objectDeclaration, primaryConstructor,
propertyDeclaration */
