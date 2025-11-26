// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.kt
private fun Int.privateExtensionFun(s: String) = s

internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFunction() = Int::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>privateExtensionFun<!>

private <!NOTHING_TO_INLINE!>inline<!> fun privateInlineFunction() = Int::privateExtensionFun
internal <!NOTHING_TO_INLINE!>inline<!> fun transitiveInlineFunction() = privateInlineFunction()

// FILE: main.kt
fun box(): String {
    return internalInlineFunction().invoke(1, "O") + transitiveInlineFunction().invoke(1, "K")
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, funWithExtensionReceiver, functionDeclaration, inline,
integerLiteral, stringLiteral */
