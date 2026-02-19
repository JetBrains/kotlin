// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

import kotlin.reflect.KFunction1

class A<T>(val t: T) {
    fun foo(): T = t
}

fun bar() {
    val x = A<String>::foo

    checkSubtype<KFunction1<A<String>, String>>(x)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, localProperty, nullableType, primaryConstructor, propertyDeclaration, typeParameter,
typeWithExtension */
