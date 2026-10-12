// ISSUE: KT-90153
// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// WITH_STDLIB
// ENABLE_JVM_PREVIEW
// JDK_KIND: FULL_JDK_21
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

// `kotlin.Number` and `kotlin.Int` are Kotlin classes whose Java classes, `java.lang.Number` and `java.lang.Integer`, are value classes.

fun number(number: Number, any: Any, ref: AtomicReference<Number>) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>number<!>) {}
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>number === any<!>
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>number<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>number<!>)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>ref.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>number<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>number<!>)<!>
}

fun <T : Number> boundedByNumber(bounded: T, any: Any) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>bounded<!>) {}
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>bounded === any<!>
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>bounded<!>)
}

fun nullableInt(x: Int?, any: Any?, ref: AtomicReference<Int?>) {
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>x === any<!>
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>x<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>x<!>)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>ref.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>x<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>x<!>)<!>
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, javaFunction, lambdaLiteral, nullableType,
typeConstraint, typeParameter */
