// RUN_PIPELINE_TILL: FRONTEND
// Can be replaced with ignore after KT-69941

import kotlin.reflect.KFunction1

class A private constructor(val s: String) {
    constructor(): this("")

    public <!NOTHING_TO_INLINE!>inline<!> fun publicInlineFunction(): KFunction1<String, A> = ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>A<!>
}

fun box(): String {
    return A().publicInlineFunction().invoke("OK").s
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inline, primaryConstructor,
propertyDeclaration, secondaryConstructor, stringLiteral */
