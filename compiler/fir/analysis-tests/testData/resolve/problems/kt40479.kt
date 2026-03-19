// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-40479

// KT-40479: Absence of completion for primaryConstructor-call when receiver is reified generic without upper-bound Any
// The issue: T::class.primaryConstructor (kotlin.reflect.full) produces UNRESOLVED_REFERENCE_WRONG_RECEIVER
// when T is reified without Any upper bound, because primaryConstructor requires KClass<out T : Any>.
// Reproduced using a self-contained extension with the same signature.

import kotlin.reflect.KClass

fun <T : Any> KClass<T>.myPrimaryConstructor(): Any? = null

inline fun <reified T> foo() {
    T::class.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>myPrimaryConstructor<!>()
}

inline fun <reified T : Any> bar() {
    T::class.myPrimaryConstructor()
}

/* GENERATED_FIR_TAGS: classReference, funWithExtensionReceiver, functionDeclaration, inline, nullableType, reified,
typeConstraint, typeParameter */
