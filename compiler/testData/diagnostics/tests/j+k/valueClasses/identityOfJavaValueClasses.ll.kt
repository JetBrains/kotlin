// LL_FIR_DIVERGENCE
// LL tests don't have jvmTargetProvider, so JDK classes are not value classes there.
// See FirJvmPlatformValueClassDeterminer
// ISSUE: KT-81100
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// WITH_STDLIB
// ENABLE_JVM_PREVIEW
// JDK_KIND: FULL_JDK_21

// FILE: JavaVal.java
public value class JavaVal {
    public final int x;

    public JavaVal(int x) {
        this.x = x;
    }
}

// FILE: JavaAbstractVal.java
public abstract value class JavaAbstractVal {}

// FILE: test.kt
import java.lang.ref.WeakReference
import java.time.LocalDate
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicReference

fun <T : JavaAbstractVal> identityEquality(v: JavaVal, date: LocalDate, abstract: JavaAbstractVal, bounded: T, record: Record, any: Any) {
    <!FORBIDDEN_IDENTITY_EQUALS!>v === JavaVal(1)<!>
    <!FORBIDDEN_IDENTITY_EQUALS!>v !== any<!>
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>date<!> === <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>LocalDate.MIN<!>
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>abstract === any<!>
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>bounded === any<!>
    record === any
}

fun structuralEquality(v: JavaVal, s: String) {
    <!EQUALITY_NOT_APPLICABLE!>v == s<!>
}

fun identitySensitiveOperations(v: JavaVal, abstract: JavaAbstractVal) {
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>v<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>v<!>)
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>JavaVal<!>, String>()
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>abstract<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>abstract<!>)
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>JavaAbstractVal<!>, String>()
}

fun atomicReference(ref: AtomicReference<JavaVal>, v: JavaVal, abstractRef: AtomicReference<JavaAbstractVal>, abstract: JavaAbstractVal) {
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>ref.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>v<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>JavaVal(2)<!>)<!>
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>abstractRef.compareAndSet(<!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>abstract<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>abstract<!>)<!>
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, integerLiteral, javaFunction, javaProperty,
javaType */
