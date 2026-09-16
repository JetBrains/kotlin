// RUN_PIPELINE_TILL: CODEGEN
fun String.foo() {}

fun String.bar() {
    foo()
}

class My {
    fun bar() {
        foo()
    }
}

fun My.foo() {}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
