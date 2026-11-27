// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
// OPT_IN: kotlin.ExperimentalValueClassesApi

import java.lang.invoke.MethodHandles
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

@JvmInline
value class Inline(val x: Int)

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

class Holder {
    @Volatile
    var any: Any? = null
}

fun fieldUpdater(updater: AtomicReferenceFieldUpdater<Holder, Any?>, inline: Inline, int: Int, date: LocalDate, wrapper: Wrapper, string: String) {
    updater.compareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inline<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inline<!>)
    updater.weakCompareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>int<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>int<!>)
    updater.compareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>date<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>date<!>)
    updater.compareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>wrapper<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>wrapper<!>)
    updater.compareAndSet(Holder(), string, string)
}

fun varHandle(inline: Inline, int: Int, date: LocalDate, wrapper: Wrapper, string: String) {
    val handle = MethodHandles.lookup().findVarHandle(Holder::class.java, "any", Any::class.java)
    handle.compareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inline<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inline<!>)
    handle.weakCompareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>int<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>int<!>)
    handle.compareAndExchange(Holder(), <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>date<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>date<!>)
    handle.compareAndSet(Holder(), <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>wrapper<!>, <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>wrapper<!>)
    handle.compareAndSet(Holder(), string, string)
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, classReference, equalityExpression, flexibleType,
functionDeclaration, isExpression, javaFunction, localProperty, nullableType, operator, override, primaryConstructor,
propertyDeclaration, smartcast, stringLiteral, value */
