// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun foo(x: Int?): Int = x!!

fun elvis(x: Number?): Int {
    val result = (x as Int?) ?: foo(x)
    checkSubtype<Int?>(x)
    return result
}


fun elvisWithRHSTypeInfo(x: Number?): Any? {
    val result = x ?: x!!
    checkSubtype<Int?>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    return result
}

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, classDeclaration, elvisExpression, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, localProperty, nullableType, propertyDeclaration, smartcast, typeParameter,
typeWithExtension */
