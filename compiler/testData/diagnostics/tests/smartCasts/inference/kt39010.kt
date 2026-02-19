// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class A<E> {
    fun foo(): E = TODO()
}

class B(var a: A<*>?) {
    fun bar() {
        if (a != null) {
            <!SMARTCAST_IMPOSSIBLE!>a<!>.foo()
        }
    }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, equalityExpression, functionDeclaration, ifExpression,
nullableType, primaryConstructor, propertyDeclaration, smartcast, starProjection, typeParameter */
