// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

context(ctx: T)
fun <T> implicit(): T = ctx

class A

context(a: A)
fun A.funMember() {
    <!AMBIGUOUS_CONTEXT_ARGUMENT!>implicit<!><A>()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
nullableType, typeParameter */
