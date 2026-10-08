// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
// OPT_IN: kotlin.ExperimentalValueClassesApi

import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

class Holder {
    @Volatile
    var wrapper: Wrapper? = null
}

fun test(ref: AtomicReference<Wrapper>, array: AtomicReferenceArray<Wrapper>, w: Wrapper, v: Wrapper) {
    ref.<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>updateAndGet<!> { it }
    ref.<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>getAndUpdate<!> { it }
    ref.<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>accumulateAndGet<!>(w) { a, _ -> a }
    ref.<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>getAndAccumulate<!>(w) { a, _ -> a }
    array.<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>updateAndGet<!>(0) { it }
    array.<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>getAndUpdate<!>(0) { it }
    array.<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>accumulateAndGet<!>(0, w) { a, _ -> a }
    array.<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>getAndAccumulate<!>(0, w) { a, _ -> a }

    val updater = AtomicReferenceFieldUpdater.newUpdater(Holder::class.java, Wrapper::class.java, "wrapper")
    updater.compareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>v<!>)
    updater.weakCompareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>v<!>)

    val handle = MethodHandles.lookup().findVarHandle(Holder::class.java, "wrapper", Wrapper::class.java)
    handle.compareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>v<!>)
    handle.weakCompareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>v<!>)
    handle.compareAndExchange(Holder(), <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>v<!>)
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, classReference, equalityExpression, flexibleType,
functionDeclaration, integerLiteral, isExpression, javaFunction, lambdaLiteral, localProperty, nullableType, operator,
override, primaryConstructor, propertyDeclaration, samConversion, smartcast, stringLiteral */
