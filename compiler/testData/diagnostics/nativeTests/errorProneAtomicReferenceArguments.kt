// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-82375
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// WITH_STDLIB

// FILE: FooValueClass.kt

<!INLINE_VALUE_CLASS_WITHOUT_EXPLICIT_INLINE_WARNING!>value<!> class Foo(val value: UInt)

// FILE: K.kt

@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.concurrent.AtomicReference

typealias KotlinAtomicReference<T> = AtomicReference<T>

fun testTypealiasedKotlin() {
    val foo = KotlinAtomicReference<Any>(Foo(1u))
    val bar = foo.value as Foo
    foo.compareAndSet(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>bar<!>, <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Foo(2u)<!>)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, asExpression, classDeclaration, classReference, flexibleType,
functionDeclaration, localProperty, nullableType, primaryConstructor, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter, unsignedLiteral, value */
