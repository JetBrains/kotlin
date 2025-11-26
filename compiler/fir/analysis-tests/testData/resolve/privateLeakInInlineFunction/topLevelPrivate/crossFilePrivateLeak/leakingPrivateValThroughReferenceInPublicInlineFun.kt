// RUN_PIPELINE_TILL: BACKEND
// FILE: A.kt
private val privateVal: String = "OK"

@Suppress(<!ERROR_SUPPRESSION!>"NON_PUBLIC_CALL_FROM_PUBLIC_INLINE"<!>)
public <!NOTHING_TO_INLINE!>inline<!> fun publicInlineFunction() = ::privateVal

// FILE: main.kt
fun box(): String {
    return publicInlineFunction().invoke()
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, inline, propertyDeclaration, stringLiteral */
