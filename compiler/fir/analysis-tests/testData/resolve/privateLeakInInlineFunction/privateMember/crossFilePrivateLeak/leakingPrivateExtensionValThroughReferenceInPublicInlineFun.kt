// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.kt
private val String.privateVal: String
    get() = this

public <!NOTHING_TO_INLINE!>inline<!> fun publicInlineFunction() = String::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVal<!>

// FILE: main.kt
fun box(): String {
    return publicInlineFunction().invoke("OK")
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, getter, inline, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, thisExpression */
