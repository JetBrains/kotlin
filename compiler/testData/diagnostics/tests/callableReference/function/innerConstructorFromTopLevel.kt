// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_EXPRESSION
import kotlin.reflect.KFunction1

class A {
    inner class Inner
}

fun main() {
    ::<!UNRESOLVED_REFERENCE!>Inner<!>
    val y = A::Inner

    checkSubtype<KFunction1<A, A.Inner>>(y)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, inner, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
