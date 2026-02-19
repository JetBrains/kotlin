// RUN_PIPELINE_TILL: BACKEND
class Foo {
    fun Foo.bar() {}

    fun test() {
        bar()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
