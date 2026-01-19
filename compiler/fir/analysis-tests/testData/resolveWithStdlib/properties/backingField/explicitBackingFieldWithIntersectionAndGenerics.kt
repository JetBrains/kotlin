// RUN_PIPELINE_TILL: BACKEND

interface I {
    val y : Any
}

open class A<T : CharSequence>(x: T) {
    val y: Any
        field: T = x

    fun testBounded(b: A<T>) {
        if (b is I) {
            b.y.length
        }
    }
}

open class B<T : CharSequence>(x: T) {
    val y: Any
        field: T = x

    fun testStar(b: B<*>) {
        if (b is I) {
            b.y.length
        }
    }
}

open class C<T : CharSequence>(x: T) {
    val y: Any
        field: T = x

    fun testViaInterface(i: I) {
        if (i is C<*>) {
            i.y.length
        }
    }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, explicitBackingField, functionDeclaration, ifExpression,
interfaceDeclaration, intersectionType, isExpression, primaryConstructor, propertyDeclaration, smartcast, starProjection,
typeConstraint, typeParameter */
