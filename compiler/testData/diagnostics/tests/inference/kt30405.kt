// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +ExpectedTypeFromCast
// CHECK_TYPE
// Issue: KT-30405

inline fun <reified T> foo(): T {
    TODO()
}

fun test() {
    val fooCall = foo() as String // T in foo should be inferred to String
    fooCall checkType { _<String>() }

    val safeFooCall = foo() as? String
    safeFooCall checkType { _<String?>() }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, inline, lambdaLiteral, localProperty, nullableType, propertyDeclaration, reified, typeParameter,
typeWithExtension */
