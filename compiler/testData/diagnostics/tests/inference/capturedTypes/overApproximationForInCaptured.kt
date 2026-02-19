// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// CHECK_TYPE

fun <T> foo(a: Array<T>): T = null!!

fun test(a: Array<in Int>) {
    foo(a) checkType { _<Any?>() }
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, inProjection, infix, lambdaLiteral, nullableType, typeParameter, typeWithExtension */
