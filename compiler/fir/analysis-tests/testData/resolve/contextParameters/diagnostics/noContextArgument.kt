// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// RENDER_DIAGNOSTICS_FULL_TEXT

context(s: String)
fun simple() {}

class C<T> {
    context(t: T)
    fun generic() {}
}

context(t: T)
fun <T> generic() {}

context(<!CONTEXT_PARAMETER_WITHOUT_NAME!>String<!>)
fun legacyContextReceiver() {}

fun test(f: context(String) () -> Unit) {
    <!NO_CONTEXT_ARGUMENT!>simple<!>()
    C<String>().<!NO_CONTEXT_ARGUMENT!>generic<!>()
    <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>generic<!>()
    <!NO_CONTEXT_ARGUMENT!>generic<!><String>()
    <!NO_CONTEXT_ARGUMENT!>f<!>()
    <!NO_CONTEXT_ARGUMENT!>legacyContextReceiver<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, functionalType,
nullableType, typeParameter, typeWithContext */
