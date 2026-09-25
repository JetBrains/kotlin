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
    v === JavaVal(1)
    v !== any
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>date<!> === <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>LocalDate.MIN<!>
    abstract === any
    bounded === any
    record === any
}

fun structuralEquality(v: JavaVal, s: String) {
    <!EQUALITY_NOT_APPLICABLE!>v == s<!>
}

fun identitySensitiveOperations(v: JavaVal, abstract: JavaAbstractVal) {
    System.identityHashCode(v)
    WeakReference(v)
    IdentityHashMap<JavaVal, String>()
    System.identityHashCode(abstract)
    WeakReference(abstract)
    IdentityHashMap<JavaAbstractVal, String>()
}

fun atomicReference(ref: AtomicReference<JavaVal>, v: JavaVal, abstractRef: AtomicReference<JavaAbstractVal>, abstract: JavaAbstractVal) {
    ref.compareAndSet(v, JavaVal(2))
    abstractRef.compareAndSet(abstract, abstract)
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, integerLiteral, javaFunction, javaProperty,
javaType */
