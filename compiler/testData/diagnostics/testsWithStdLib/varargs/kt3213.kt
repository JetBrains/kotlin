// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun test(a: Array<out String>) {
    val b = a.toList()

    b checkType { _<List<String>>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
lambdaLiteral, localProperty, nullableType, outProjection, propertyDeclaration, typeParameter, typeWithExtension */
