// RUN_PIPELINE_TILL: CODEGEN
class C {
    fun Int.foo() {}
}

fun C.foo() {}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
