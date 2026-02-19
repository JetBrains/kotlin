// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82375
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// WITH_STDLIB

// FILE: FooValueClass.kt

@JvmInline
value class Foo(val value: UInt)

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReference

typealias JavaAtomicReference<T> = AtomicReference<T>

fun testTypealiasedJava() {
    val foo = JavaAtomicReference<Any>(Foo(1u))
    val bar = foo.get() as Foo
    foo.compareAndSet(bar, Foo(2u))
}

// FILE: K.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference

typealias KotlinAtomicReference<T> = AtomicReference<T>

fun testTypealiasedKotlin() {
    val foo = KotlinAtomicReference<Any>(Foo(1u))
    val bar = foo.load() as Foo
    foo.compareAndSet(bar, Foo(2u))
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, asExpression, classDeclaration, classReference, flexibleType,
functionDeclaration, localProperty, nullableType, primaryConstructor, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter, unsignedLiteral, value */
