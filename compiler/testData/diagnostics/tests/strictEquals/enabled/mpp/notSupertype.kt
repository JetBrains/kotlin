// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: commonFile.kt
package p

interface I

expect class C {
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>I<!>::class) other: Any?): Boolean
}

expect open class D()

class E : D() {
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>I<!>::class) other: Any?): Boolean = true
}

expect interface T

class F : I {
    override fun equals(@EqualityBound(<!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>T<!>::class) other: Any?): Boolean = true
}

// MODULE: platform()()(common)
// FILE: platformFile.kt
package p

actual class C : I {
    actual override fun equals(@EqualityBound(I::class) other: Any?): Boolean = true
}

actual open class D : I

actual typealias T = I

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration, interfaceDeclaration,
nullableType, operator, override */
