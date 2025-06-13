// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

fun test() {
    val array = arrayOf(arrayOf(1))
    array checkType { _<Array<Array<Int>>>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
