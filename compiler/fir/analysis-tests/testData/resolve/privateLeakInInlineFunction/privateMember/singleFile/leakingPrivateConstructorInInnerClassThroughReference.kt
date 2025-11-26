// RUN_PIPELINE_TILL: FRONTEND
import kotlin.reflect.KFunction1

class A {
    inner class Inner private constructor(val s: String) {
        constructor(): this("")

        internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineMethod(): KFunction1<String, Inner> = ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>Inner<!>
    }
}

fun box(): String {
    return A().Inner().internalInlineMethod().invoke("OK").s
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inline, inner, primaryConstructor,
propertyDeclaration, secondaryConstructor, stringLiteral */
