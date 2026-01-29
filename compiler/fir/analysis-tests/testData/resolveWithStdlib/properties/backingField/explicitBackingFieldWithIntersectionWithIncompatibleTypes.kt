// RUN_PIPELINE_TILL: BACKEND
interface I {
    val y : Any
}

open class C<T>(x: T) {
    val y: Any?
        field: T = x

    fun test(b: C<String>) {
        if (b is I) {
            b.y.length
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, ifExpression, interfaceDeclaration,
intersectionType, isExpression, nullableType, primaryConstructor, propertyDeclaration, smartcast, typeParameter */
