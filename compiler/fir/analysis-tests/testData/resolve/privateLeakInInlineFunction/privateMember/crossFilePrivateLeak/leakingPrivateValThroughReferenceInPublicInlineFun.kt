// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.kt
class A constructor(val s: String) {
    private val privateVal: String = s

    public <!NOTHING_TO_INLINE!>inline<!> fun publicInlineFunction() = ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVal<!>
}

// FILE: main.kt
fun box(): String {
    return A("OK").publicInlineFunction().invoke()
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inline, primaryConstructor,
propertyDeclaration, stringLiteral */
