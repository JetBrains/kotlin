// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// ISSUE: KT-61937
// LANGUAGE: +ContextReceivers, -ContextParameters

class Ctx

context(Ctx)
fun Ctx.foo(): String = "NOK"

context(Ctx)
fun bar(foo: Ctx.() -> String ): String {
    return foo<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun box(): String = with (Ctx()) {
    bar { "OK" }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
functionalType, lambdaLiteral, stringLiteral, typeWithExtension */
