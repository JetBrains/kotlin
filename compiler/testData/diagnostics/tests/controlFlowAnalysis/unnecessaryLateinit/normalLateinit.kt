// RUN_PIPELINE_TILL: BACKEND
class Foo {
    lateinit var bar: String

    fun init() {
        bar = ""
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, lateinit, propertyDeclaration, stringLiteral */
