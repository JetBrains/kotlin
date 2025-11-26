// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.kt
class A {
    private fun privateFun(s: String) = s

    internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFunction() = ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>privateFun<!>

    private <!NOTHING_TO_INLINE!>inline<!> fun privateInlineFunction() = ::privateFun
    internal <!NOTHING_TO_INLINE!>inline<!> fun transitiveInlineFunction() = privateInlineFunction()
}

// FILE: main.kt
fun box(): String {
    return A().internalInlineFunction().invoke("O") + A().transitiveInlineFunction().invoke("K")
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, classDeclaration, functionDeclaration, inline,
stringLiteral */
