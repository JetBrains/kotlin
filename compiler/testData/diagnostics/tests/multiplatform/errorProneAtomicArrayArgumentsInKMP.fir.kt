// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82375
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB

// FILE: FooValueClass.kt

@JvmInline
value class Foo(val value: UInt)

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReferenceArray

fun testJavaArray() {
    val foo = AtomicReferenceArray<Any>(1)
    val bar = foo.get(0) as Foo
    foo.compareAndSet(0, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>bar<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Foo(2u)<!>)
}

// FILE: K.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicArray

fun testKotlinArray() {
    val foo = AtomicArray(arrayOf<Any>(Foo(1u)))
    val bar = foo.loadAt(0) as Foo
    foo.compareAndSetAt(0, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>bar<!>, <!ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY!>Foo(2u)<!>)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, assignment, classReference, flexibleType, functionDeclaration,
integerLiteral, javaFunction, localProperty, propertyDeclaration */
