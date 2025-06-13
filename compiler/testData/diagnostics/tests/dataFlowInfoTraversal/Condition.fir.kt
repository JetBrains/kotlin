// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun foo(x: Int?): Boolean {
    val result = ((x!! == 0) && (checkSubtype<Int>(x) == 0))
    checkSubtype<Int>(x)
    return result
}

/* GENERATED_FIR_TAGS: andExpression, checkNotNullCall, classDeclaration, equalityExpression, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, integerLiteral, localProperty, nullableType, propertyDeclaration, smartcast,
typeParameter, typeWithExtension */
