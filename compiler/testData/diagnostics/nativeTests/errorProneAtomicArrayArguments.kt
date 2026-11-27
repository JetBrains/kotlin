// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-82375
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: FooValueClass.kt

<!INLINE_VALUE_CLASS_WITHOUT_EXPLICIT_INLINE_WARNING!>value<!> class Foo(val value: UInt)

// FILE: K.kt

@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.concurrent.AtomicArray

fun testKotlinArray() {
    val foo = AtomicArray<Any>(1) { Foo(1u) }
    val bar = foo.get(0) as Foo
    foo.compareAndSet(0, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>bar<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Foo(2u)<!>)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, assignment, classReference, flexibleType, functionDeclaration,
integerLiteral, javaFunction, localProperty, propertyDeclaration */
