// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun foo(x: Number) {
    if (<!USELESS_IS_CHECK!>(x as Int) is Int<!>) {
        checkSubtype<Int>(x)
    }
    checkSubtype<Int>(x)
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, infix, isExpression, nullableType, smartcast, typeParameter, typeWithExtension */
