// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83589

interface I {
    val y: Any
}

open class A(x: String) {
    val y: Any
        field: String = x

    fun test(x: A?) {
        if (x is I) {
            println(x.y.length)
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, ifExpression, interfaceDeclaration,
intersectionType, isExpression, nullableType, primaryConstructor, propertyDeclaration, smartcast */
