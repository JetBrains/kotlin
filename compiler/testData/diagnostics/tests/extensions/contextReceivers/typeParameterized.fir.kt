// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters
// DIAGNOSTICS: -UNUSED_PARAMETER

class A
class B<X>(val x: X)

context(T)
fun <T> T.f(t: B<T>) {}

fun Int.main(a: A, b: B<String>) {
    a.f(<!ARGUMENT_TYPE_MISMATCH!>b<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
nullableType, primaryConstructor, propertyDeclaration, typeParameter */
