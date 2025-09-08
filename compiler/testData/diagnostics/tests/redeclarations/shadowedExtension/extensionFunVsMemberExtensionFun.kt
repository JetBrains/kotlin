// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class C {
    fun Int.foo() {}
}

fun C.foo() {}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
