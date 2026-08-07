// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

import java.lang.ref.WeakReference
import java.util.IdentityHashMap
import java.util.WeakHashMap

@WillBecomeValue
class Key(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Key && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Key($x)"
}

fun test(key: Key) {
    synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>key<!>) { }
    System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>key<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>key<!>)
    IdentityHashMap<<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>Key<!>, String>()
    WeakHashMap<<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>Key<!>, String>()
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, flexibleType, functionDeclaration,
isExpression, javaFunction, lambdaLiteral, nullableType, operator, override, primaryConstructor, propertyDeclaration,
smartcast */
