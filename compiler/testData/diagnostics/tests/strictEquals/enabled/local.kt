// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

fun local(): Any {
    open class A
    class B : A() {
        override fun equals(@EqualityBound(bound = A::class) other: Any?): Boolean = true
    }

    return object : A() {
        override fun equals(@EqualityBound(A::class) other: Any?): Boolean = true
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, classReference, functionDeclaration, localClass,
nullableType, operator */
