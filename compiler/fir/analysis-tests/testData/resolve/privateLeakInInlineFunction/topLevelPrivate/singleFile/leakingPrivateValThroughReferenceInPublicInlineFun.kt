// RUN_PIPELINE_TILL: BACKEND
private val privateVal: String = "OK"

@Suppress(<!ERROR_SUPPRESSION!>"NON_PUBLIC_CALL_FROM_PUBLIC_INLINE"<!>)
public <!NOTHING_TO_INLINE!>inline<!> fun publicInlineFunction() = ::privateVal

fun box(): String {
    return publicInlineFunction().invoke()
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, inline, propertyDeclaration, stringLiteral */
