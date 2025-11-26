// RUN_PIPELINE_TILL: FRONTEND
private fun Int.privateExtensionFun() = "OK"

public <!NOTHING_TO_INLINE!>inline<!> fun publicInlineFunction() = Int::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateExtensionFun<!>

fun box(): String {
    return publicInlineFunction().invoke(1)
}

/* GENERATED_FIR_TAGS: callableReference, funWithExtensionReceiver, functionDeclaration, inline, integerLiteral,
stringLiteral */
