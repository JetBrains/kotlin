// RUN_PIPELINE_TILL: BACKEND
class Array<E>(e: E) {
    val k = Array(1) {
        1 as Any
        e as Any?
    }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, integerLiteral, lambdaLiteral, nullableType, primaryConstructor,
propertyDeclaration, typeParameter */
