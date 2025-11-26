// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.kt
class A constructor(val s: String) {
    private val privateVal: String = s

    internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFunction() = ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>privateVal<!>

    private <!NOTHING_TO_INLINE!>inline<!> fun privateInlineFunction() = ::privateVal
    internal <!NOTHING_TO_INLINE!>inline<!> fun transitiveInlineFunction() = privateInlineFunction()
}

// FILE: main.kt
fun box(): String {
    return A("O").internalInlineFunction().invoke() + A("K").transitiveInlineFunction().invoke()
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, classDeclaration, functionDeclaration, inline,
primaryConstructor, propertyDeclaration, stringLiteral */
