// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.kt
class A {
    private var privateVar = 22

    public <!NOTHING_TO_INLINE!>inline<!> fun publicInlineFunction() = ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVar<!>
}

// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        result += publicInlineFunction().get()
        publicInlineFunction().set(20)
        result += publicInlineFunction().get()
    }
    if (result != 42) return result.toString()
    return "OK"
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, callableReference, classDeclaration, equalityExpression,
functionDeclaration, ifExpression, inline, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration,
stringLiteral */
