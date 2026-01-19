// RUN_PIPELINE_TILL: FRONTEND

interface I<T> {
    val y: T
}

open class A<T>(x: T&Any) {
    val y: T
        field: T&Any = x

    fun <T> test(x: A<T>) {
        if (x is I<*>) {
            val k: T&Any <!INITIALIZER_TYPE_MISMATCH!>=<!> x.y
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, explicitBackingField, functionDeclaration, ifExpression,
interfaceDeclaration, intersectionType, isExpression, localProperty, nullableType, primaryConstructor,
propertyDeclaration, smartcast, starProjection, typeParameter */
