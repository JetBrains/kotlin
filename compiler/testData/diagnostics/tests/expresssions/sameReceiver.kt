// RUN_PIPELINE_TILL: CODEGEN
class Foo {
    fun Foo.bar() {}

    fun test() {
        bar()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
