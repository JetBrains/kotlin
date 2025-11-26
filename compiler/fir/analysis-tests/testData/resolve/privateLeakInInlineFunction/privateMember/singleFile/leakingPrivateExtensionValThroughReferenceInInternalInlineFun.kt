// RUN_PIPELINE_TILL: FRONTEND
private val String.privateVal: String
    get() = this

internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFunction() = String::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>privateVal<!>

private <!NOTHING_TO_INLINE!>inline<!> fun privateInlineFunction() = String::privateVal
internal <!NOTHING_TO_INLINE!>inline<!> fun transitiveInlineFunction() = privateInlineFunction()

fun box(): String {
    return internalInlineFunction().invoke("O") + transitiveInlineFunction().invoke("K")
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, functionDeclaration, getter, inline, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, thisExpression */
