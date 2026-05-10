// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_EXPRESSION

import kotlin.reflect.KFunction1

class A {
    inner class Inner
}
    
fun A.main() {
    ::Inner
    val y = A::Inner

    checkSubtype<KFunction1<A, A.Inner>>(y)
}

fun Int.main() {
    ::<!UNRESOLVED_REFERENCE!>Inner<!>
    val y = A::Inner

    checkSubtype<KFunction1<A, A.Inner>>(y)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, inner, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
