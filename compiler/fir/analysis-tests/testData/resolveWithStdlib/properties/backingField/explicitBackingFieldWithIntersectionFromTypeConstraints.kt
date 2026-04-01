// RUN_PIPELINE_TILL: BACKEND

interface I {
    val y : Any
}

open class A(x: String) {
    val y: Any
        field: String = x

    fun <T> test(x: T?) where T : A, T : I {
        if (x != null) {
            x.y.length
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, equalityExpression, explicitBackingField, functionDeclaration,
ifExpression, interfaceDeclaration, nullableType, primaryConstructor, propertyDeclaration, smartcast, typeConstraint,
typeParameter */
