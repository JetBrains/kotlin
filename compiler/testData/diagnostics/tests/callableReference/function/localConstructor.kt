// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

import kotlin.reflect.KFunction0

fun main() {
    class A
    
    val x = ::A
    checkSubtype<KFunction0<A>>(x)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, localClass, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
