// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

fun test(vararg a: String) {
    a checkType { _<Array<out String>>() }

    foo(a) checkType { _<Array<out String>>() }
}

fun <T> test1(vararg t: T) {
    t checkType { _<Array<out T>>() }
}

fun <T> foo(a: Array<T>): Array<T> = a

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, lambdaLiteral, nullableType, outProjection, typeParameter, typeWithExtension, vararg */
