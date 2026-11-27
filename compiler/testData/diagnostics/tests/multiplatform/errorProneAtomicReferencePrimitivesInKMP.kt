// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

// MODULE: common
// FILE: C.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference

fun commonTestKotlin() {
    val k = AtomicReference<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY, ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY{METADATA}!>k.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE{METADATA}!>127<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE{METADATA}!>128<!>)<!> // true
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY, ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY{METADATA}!>k.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE{METADATA}!>128<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE{METADATA}!>7777<!>)<!> // false

    val kk: AtomicReference<Int>
    kk = k
}

expect class KotlinAtomicReference<T>(value: T) {
    fun compareAndSet(expectedValue: T, newValue: T): Boolean
}

fun commonTypealiasedKotlin() {
    val k = KotlinAtomicReference<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>k.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>127<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>128<!>)<!> // true
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>k.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>128<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>7777<!>)<!> // false

    val kk: KotlinAtomicReference<Int>
    kk = k
}

// MODULE: jvm()()(common)
// FILE: K.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference

fun testKotlin() {
    val k = AtomicReference<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>k.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>127<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>128<!>)<!> // true
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>k.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>128<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>7777<!>)<!> // false

    val kk: AtomicReference<Int>
    kk = k
}

actual typealias KotlinAtomicReference<T> = AtomicReference<T>

fun testTypealiasedKotlin() {
    val k = KotlinAtomicReference<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>k.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>127<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>128<!>)<!> // true
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>k.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>128<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>7777<!>)<!> // false

    val kk: KotlinAtomicReference<Int>
    kk = k
}

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReference

fun testJava() {
    val j = AtomicReference<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>j.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>127<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>128<!>)<!> // true
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>j.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>128<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>7777<!>)<!> // false

    val jj: AtomicReference<Int>
    jj = j
}

/* GENERATED_FIR_TAGS: actual, annotationUseSiteTargetFile, assignment, classDeclaration, classReference, expect,
flexibleType, functionDeclaration, integerLiteral, javaFunction, localProperty, nullableType, primaryConstructor,
propertyDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
