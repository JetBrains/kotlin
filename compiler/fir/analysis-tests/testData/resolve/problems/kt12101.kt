// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-12101
// WITH_STDLIB

// KT-12101: Confusing error message for TYPE_PARAMETER_AS_REIFIED in 'arrayOf<T>' and similar calls
class Foo<E: Any>() {
    fun foo() {
        arrayOf<<!TYPE_PARAMETER_AS_REIFIED!>E?<!>>()
        Array<<!TYPE_PARAMETER_AS_REIFIED!>E?<!>>(size = 1, init = { i -> null })
        arrayOfNulls<<!TYPE_PARAMETER_AS_REIFIED!>E<!>>(6)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, lambdaLiteral, nullableType,
primaryConstructor, typeConstraint, typeParameter */
