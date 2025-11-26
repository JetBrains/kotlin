// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.kt
class A {
    private var privateVar = 22

    internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFunction() = ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>privateVar<!>
}

// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        result += internalInlineFunction().get()
        internalInlineFunction().set(20)
        result += internalInlineFunction().get()
    }
    if (result != 42) return result.toString()
    return "OK"
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, callableReference, classDeclaration, equalityExpression,
functionDeclaration, ifExpression, inline, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration,
stringLiteral */
