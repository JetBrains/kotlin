// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-21598

class Foo {
    @Deprecated(message = "For binary compatibility only", level = DeprecationLevel.HIDDEN)
    fun bar() {}
}

fun Foo.bar() {}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, stringLiteral */
