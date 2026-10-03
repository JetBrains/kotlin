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
    <!FORBIDDEN_IDENTITY_EQUALS!>date === LocalDate.MIN<!>
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>abstract === any<!>
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>bounded === any<!>
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>record === any<!>
}

fun structuralEquality(v: JavaVal, s: String) {
    <!EQUALITY_NOT_APPLICABLE!>v == s<!>
}

fun identitySensitiveOperations(v: JavaVal, abstract: JavaAbstractVal) {
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>v<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>v<!>)
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>JavaVal<!>, String>()
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>abstract<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>abstract<!>)
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>JavaAbstractVal<!>, String>()
}

fun <T : JavaAbstractVal> identitySensitiveOperationsThroughBounds(bounded: T) {
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>bounded<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>bounded<!>)
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>T<!>, String>()
}

fun atomicReference(ref: AtomicReference<JavaVal>, v: JavaVal, abstractRef: AtomicReference<JavaAbstractVal>, abstract: JavaAbstractVal) {
    ref.compareAndSet(v, JavaVal(2))
    abstractRef.compareAndSet(abstract, abstract)
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, integerLiteral, javaFunction, javaProperty,
javaType, typeConstraint, typeParameter */
