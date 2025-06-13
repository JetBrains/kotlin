// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

import kotlin.reflect.KFunction1

class A {
    fun foo() = 42
}

fun A.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() {}

fun main() {
    val x = A::foo

    checkSubtype<KFunction1<A, Int>>(x)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, integerLiteral, localProperty, nullableType, propertyDeclaration, typeParameter,
typeWithExtension */
