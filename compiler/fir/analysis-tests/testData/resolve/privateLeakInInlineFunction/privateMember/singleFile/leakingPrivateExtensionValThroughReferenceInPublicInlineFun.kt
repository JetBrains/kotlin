// RUN_PIPELINE_TILL: FRONTEND
private val String.privateVal: String
    get() = this

public <!NOTHING_TO_INLINE!>inline<!> fun publicInlineFunction() = String::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVal<!>

fun box(): String {
    return publicInlineFunction().invoke("OK")
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, getter, inline, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, thisExpression */
