// RUN_PIPELINE_TILL: FRONTEND
class A {
    private fun privateMethod() = "OK"

    public <!NOTHING_TO_INLINE!>inline<!> fun publicInlineFunction() = ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateMethod<!>
}

fun box(): String {
    return A().publicInlineFunction().invoke()
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inline, stringLiteral */
