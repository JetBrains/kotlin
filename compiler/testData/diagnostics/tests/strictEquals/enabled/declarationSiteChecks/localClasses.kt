// RUN_PIPELINE_TILL: FRONTEND

interface I1

interface I2 : I1 {
    override fun equals(@EqualityBound(I2::class) other: Any?): Boolean
}

open class AC : I1 {
    override fun equals(@EqualityBound(I1::class) other: Any?): Boolean = true
}

fun test() {
    val obj = <!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE!>object<!> : AC(), I2 { }
    class Local : I2 {
        <!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE!>override fun equals(@EqualityBound(I1::class) other: Any?): Boolean = true<!>
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, classReference, functionDeclaration,
interfaceDeclaration, localClass, localProperty, nullableType, operator, override, propertyDeclaration */
