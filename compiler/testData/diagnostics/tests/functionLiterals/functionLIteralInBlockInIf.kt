// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
fun test() {
    val a = if (true) {
        val x = 1
        ({ x })
    } else {
        { 2 }
    }
    a checkType {  _<() -> Int>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression,
infix, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
