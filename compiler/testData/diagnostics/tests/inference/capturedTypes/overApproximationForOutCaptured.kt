// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// CHECK_TYPE

fun <T> foo(a: Array<T>): T = null!!

fun test(a: Array<out Int>) {
    foo(a) checkType { _<Int>() }
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, lambdaLiteral, nullableType, outProjection, typeParameter, typeWithExtension */
