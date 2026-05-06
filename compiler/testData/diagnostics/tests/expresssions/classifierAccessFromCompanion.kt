// RUN_PIPELINE_TILL: FRONTEND
class Factory {
    sealed class Function {
        object Default
    }

    companion object {
        val f = <!NO_COMPANION_OBJECT!>Function<!>
        val x = Function.Default
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, nestedClass, objectDeclaration, propertyDeclaration, sealed */
