// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

fun test2() {
    val x = run f@{return@f 1}
    checkSubtype<Int>(x)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
