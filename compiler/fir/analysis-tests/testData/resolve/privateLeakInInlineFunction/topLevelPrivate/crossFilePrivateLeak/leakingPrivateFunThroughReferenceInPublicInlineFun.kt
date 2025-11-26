// RUN_PIPELINE_TILL: BACKEND
// FILE: A.kt
private fun privateFun() = "OK"

@Suppress(<!ERROR_SUPPRESSION!>"NON_PUBLIC_CALL_FROM_PUBLIC_INLINE"<!>)
public <!NOTHING_TO_INLINE!>inline<!> fun publicInlineFunction() = ::privateFun

// FILE: main.kt
fun box(): String {
    return publicInlineFunction().invoke()
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, inline, stringLiteral */
