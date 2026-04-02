// RUN_PIPELINE_TILL: BACKEND
enum class E {
    FIRST,

    SECOND;

    companion object {
        class FIRST

        val SECOND = this
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, nestedClass, objectDeclaration,
propertyDeclaration, thisExpression */
