// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
// OPT_IN: kotlin.ExperimentalValueClassesApi

import java.lang.ref.PhantomReference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.atomic.AtomicStampedReference

@JvmInline
value class Inline(val x: Int)

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

fun stamped(
    inlines: AtomicStampedReference<Inline>, ints: AtomicStampedReference<Int>, dates: AtomicStampedReference<LocalDate>,
    wrappers: AtomicStampedReference<Wrapper>, strings: AtomicStampedReference<String>,
    inline: Inline, int: Int, date: LocalDate, wrapper: Wrapper, string: String,
) {
    inlines.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inline<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inline<!>, 0, 1)
    ints.weakCompareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>int<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>int<!>, 0, 1)
    dates.attemptStamp(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>date<!>, 1)
    wrappers.compareAndSet(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>wrapper<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>wrapper<!>, 0, 1)
    strings.compareAndSet(string, string, 0, 1)
}

fun markable(
    inlines: AtomicMarkableReference<Inline>, ints: AtomicMarkableReference<Int>, dates: AtomicMarkableReference<LocalDate>,
    wrappers: AtomicMarkableReference<Wrapper>, strings: AtomicMarkableReference<String>,
    inline: Inline, int: Int, date: LocalDate, wrapper: Wrapper, string: String,
) {
    inlines.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inline<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inline<!>, false, true)
    ints.weakCompareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>int<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>int<!>, false, true)
    dates.attemptMark(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>date<!>, true)
    wrappers.compareAndSet(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>wrapper<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>wrapper<!>, false, true)
    strings.compareAndSet(string, string, false, true)
}

fun references(
    weak: WeakReference<Any>, soft: SoftReference<Any>, phantom: PhantomReference<Any>,
    inline: Inline, int: Int, date: LocalDate, wrapper: Wrapper, string: String,
) {
    weak.refersTo(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inline<!>)
    soft.refersTo(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>int<!>)
    phantom.refersTo(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>date<!>)
    weak.refersTo(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>wrapper<!>)
    weak.refersTo(string)
    weak.refersTo(null)
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, integerLiteral,
isExpression, nullableType, operator, override, primaryConstructor, propertyDeclaration, smartcast, stringLiteral, value */
