// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

import kotlin.reflect.KProperty1

class C {
    val baz: Int = 12
}

fun Int.baz() {}

fun test() {
    C::baz checkType { _<KProperty1<C, Int>>() }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, integerLiteral, lambdaLiteral, nullableType, propertyDeclaration, typeParameter,
typeWithExtension */
