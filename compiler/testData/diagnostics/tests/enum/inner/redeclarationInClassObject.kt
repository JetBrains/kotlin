// RUN_PIPELINE_TILL: BACKEND
class A {
    enum class E {
        ENTRY
    }
    
    companion object {
        enum class E {
            ENTRY2
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, nestedClass, objectDeclaration */
