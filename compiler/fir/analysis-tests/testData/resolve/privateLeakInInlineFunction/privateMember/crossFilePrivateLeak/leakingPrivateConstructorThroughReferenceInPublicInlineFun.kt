// RUN_PIPELINE_TILL: FRONTEND
import kotlin.reflect.KFunction1

// FILE: A.kt
class A private constructor(val s: String) {
    constructor(): this("")

    public <!NOTHING_TO_INLINE!>inline<!> fun publicInlineFunction(): KFunction1<String, A> = ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>A<!>
}

// FILE: main.kt
fun box(): String {
    return A().publicInlineFunction().invoke("OK").s
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inline, primaryConstructor,
propertyDeclaration, secondaryConstructor, stringLiteral */
