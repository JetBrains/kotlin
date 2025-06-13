// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun foo(arr: Array<Int>?) {
    for (x in arr!!) {
        checkSubtype<Array<Int>>(arr)
    }
    checkSubtype<Array<Int>>(arr)
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, forLoop, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, localProperty, nullableType, propertyDeclaration, smartcast, typeParameter, typeWithExtension */
