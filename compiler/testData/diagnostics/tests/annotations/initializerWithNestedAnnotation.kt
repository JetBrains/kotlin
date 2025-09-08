// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
annotation class Anno(val position: String)

class MyClass {
    <!WRONG_ANNOTATION_TARGET!>@Anno("init $prop")<!> init {

    }

    companion object {
        private const val prop = 0
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, const, init, integerLiteral,
objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
