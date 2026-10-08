// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

import kotlin.concurrent.atomics.AtomicReference

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

fun test(ref: AtomicReference<Wrapper>, javaRef: java.util.concurrent.atomic.AtomicReference<Wrapper>, w: Wrapper) {
    ref.compareAndSet(w, w)
    ref.compareAndExchange(w, w)
    javaRef.compareAndSet(w, w)
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, isExpression,
nullableType, operator, override, primaryConstructor, propertyDeclaration, smartcast, stringLiteral */
