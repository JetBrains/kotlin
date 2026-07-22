// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: FRONTEND

// MODULE: common
// FILE: file1.kt
package example

import kotlin.reflect.KClass

expect annotation class MyEqualityBound(val bound: KClass<*>)

class Foo {
    override fun equals(@MyEqualityBound(Foo::class) other: Any?): Boolean = true
}

// MODULE: jvm()()(common)
// FILE: file2.kt
package example

actual typealias MyEqualityBound = <!ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION, TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_ERROR!>EqualityBound<!>

class Bar {
    override fun equals(@MyEqualityBound(Bar::class) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, classDeclaration, classReference, expect, functionDeclaration,
nullableType, operator, override, primaryConstructor, propertyDeclaration, starProjection, typeAliasDeclaration */
