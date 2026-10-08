// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
// OPT_IN: kotlin.ExperimentalValueClassesApi

import java.lang.ref.PhantomReference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.atomic.AtomicStampedReference

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

fun atomics(stamped: AtomicStampedReference<Wrapper>, markable: AtomicMarkableReference<Wrapper>, w: Wrapper, v: Wrapper) {
    stamped.compareAndSet(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>v<!>, 0, 1)
    stamped.weakCompareAndSet(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>v<!>, 0, 1)
    stamped.attemptStamp(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, 1)
    markable.compareAndSet(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>v<!>, false, true)
    markable.weakCompareAndSet(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>v<!>, false, true)
    markable.attemptMark(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>, true)
}

fun references(weak: WeakReference<Wrapper>, soft: SoftReference<Wrapper>, phantom: PhantomReference<Wrapper>, w: Wrapper) {
    weak.refersTo(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>)
    soft.refersTo(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>)
    phantom.refersTo(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w<!>)
    weak.refersTo(null)
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, integerLiteral,
isExpression, nullableType, operator, override, primaryConstructor, propertyDeclaration, smartcast, stringLiteral */
