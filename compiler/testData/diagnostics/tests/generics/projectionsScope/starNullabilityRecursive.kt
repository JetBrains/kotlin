// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// CHECK_TYPE
interface A<T : A<T?>?> {
    fun foo(): T?
}
fun testA(a: A<*>) {
    a.foo() checkType { _<A<*>?>() }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, interfaceDeclaration, lambdaLiteral, nullableType, starProjection, typeConstraint, typeParameter,
typeWithExtension */
