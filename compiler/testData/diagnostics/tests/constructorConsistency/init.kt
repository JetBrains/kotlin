// RUN_PIPELINE_TILL: CODEGEN
class My {
    val x: String

    init {
        x = foo()
    }

    fun foo(): String = x
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, init, propertyDeclaration */
