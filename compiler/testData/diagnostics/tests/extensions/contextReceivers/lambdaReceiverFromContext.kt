// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED, -CONTEXT_CLASS_OR_CONSTRUCTOR
// LANGUAGE: +ContextReceivers, -ContextParameters

class Ctx

fun Ctx.foo() {}

context(Ctx)
class A {
    fun bar(body: Ctx.() -> Unit) {
        foo()
        body<!NO_VALUE_FOR_PARAMETER!>()<!>
    }
}

context(Ctx)
fun bar(body: Ctx.() -> Unit) {
    foo()
    body<!NO_VALUE_FOR_PARAMETER!>()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
functionalType, typeWithExtension */
