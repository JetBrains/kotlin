// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A<T> {
    fun foo() {
        val q = object {
            open inner class B
            inner class C : B()

            // No WRONG_NUMBER_OF_TYPE_ARGUMENTS should be reported on these types
            val x: B = B()
            val y: C = C()
        }

        q.x
        q.y
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, inner, localClass,
localProperty, nullableType, propertyDeclaration, typeParameter */
