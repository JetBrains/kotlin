// RUN_PIPELINE_TILL: FRONTEND
import kotlin.reflect.KFunction1

class A private constructor(val s: String) {
    constructor(): this("")

    internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFunction(): KFunction1<String, A> = ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>A<!>

    private <!NOTHING_TO_INLINE!>inline<!> fun privateInlineFunction(): KFunction1<String, A> = ::A
    internal <!NOTHING_TO_INLINE!>inline<!> fun transitiveInlineFunction() = privateInlineFunction()
}

fun box(): String {
    return A().internalInlineFunction().invoke("O").s + A().transitiveInlineFunction().invoke("K").s
}

/* GENERATED_FIR_TAGS: additiveExpression, callableReference, classDeclaration, functionDeclaration, inline,
primaryConstructor, propertyDeclaration, secondaryConstructor, stringLiteral */
