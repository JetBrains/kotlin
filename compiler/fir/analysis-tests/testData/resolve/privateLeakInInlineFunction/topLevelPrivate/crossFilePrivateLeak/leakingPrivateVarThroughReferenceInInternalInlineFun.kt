// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.kt
private var privateVar: String = ""

internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFunction() = ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>privateVar<!>

private <!NOTHING_TO_INLINE!>inline<!> fun privateInlineFunction() = ::privateVar
internal <!NOTHING_TO_INLINE!>inline<!> fun transitiveInlineFunction() = privateInlineFunction()

// FILE: main.kt
fun box(): String {
    return internalInlineFunction().apply { set("O") }.get() + transitiveInlineFunction().apply { set("K") }.get()
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, functionDeclaration, inline, lambdaLiteral,
propertyDeclaration, stringLiteral */
