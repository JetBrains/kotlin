// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

class A<T> {
    fun foo(): T = null!!
}

fun <E> A<E>.bar(): A<in E> = this

fun baz(x: A<out CharSequence>) {
    x.bar() checkType { _<A<*>>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x.bar().foo()<!> checkType { _<Any?>() } // See KT-10448
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x.bar().foo()<!> checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><CharSequence>() } // Inference change in K2
}

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, inProjection, infix, lambdaLiteral, nullableType, outProjection, starProjection, thisExpression,
typeParameter, typeWithExtension */
