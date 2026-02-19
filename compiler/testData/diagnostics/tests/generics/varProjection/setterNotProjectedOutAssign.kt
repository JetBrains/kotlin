// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
interface Tr<T> {
    var v: Tr<T>
}

fun test(t: Tr<*>) {
    <!SETTER_PROJECTED_OUT!>t.v<!> = t
    t.v checkType { _<Tr<*>>() }
}

/* GENERATED_FIR_TAGS: assignment, capturedType, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, interfaceDeclaration, lambdaLiteral, nullableType, propertyDeclaration, starProjection,
typeParameter, typeWithExtension */
