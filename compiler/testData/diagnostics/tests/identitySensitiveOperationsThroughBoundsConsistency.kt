// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
// OPT_IN: kotlin.ExperimentalValueClassesApi

import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

@JvmInline
value class Inline(val x: Int)

@WillBecomeValue
abstract class Base

class Holder {
    @Volatile
    var any: Any? = null
}

fun <T : <!FINAL_UPPER_BOUND!>Inline<!>> atomicReference(reference: AtomicReference<Any?>, t: T) {
    reference.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>)
}

fun <T : <!FINAL_UPPER_BOUND!>Inline<!>> fieldUpdater(updater: AtomicReferenceFieldUpdater<Holder, Any?>, t: T) {
    updater.compareAndSet(Holder(), <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>)
}

fun <T : <!FINAL_UPPER_BOUND!>Inline<!>> varHandle(t: T) {
    val handle = MethodHandles.lookup().findVarHandle(Holder::class.java, "any", Any::class.java)
    handle.compareAndSet(Holder(), <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>t<!>)
}

fun <T : Base> willBecomeValue(updater: AtomicReferenceFieldUpdater<Holder, Any?>, t: T) {
    updater.compareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>t<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>t<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, flexibleType, functionDeclaration, javaFunction, localProperty,
nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeConstraint, typeParameter, value */
