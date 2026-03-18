// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-18295
// WITH_STDLIB

// KT-18295: Operator convention satisfied with a variable of function with receiver type, exception in back-end
fun <T> printMe(args: T, iterator: T.() -> Iterator<*>) {
    for (item in <!NOT_FUNCTION_AS_OPERATOR!>args<!>) {
        println(item)
    }
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, functionalType, localProperty, nullableType, propertyDeclaration,
starProjection, typeParameter, typeWithExtension */
