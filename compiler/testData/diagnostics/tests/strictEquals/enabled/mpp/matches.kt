// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: commonFile.kt
package p

expect class A {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean
}

abstract class Super {
    override fun equals(@EqualityBound(Super::class) other: Any?): Boolean = true
}

expect class ExplicitInCommon : Super {
    override fun equals(@EqualityBound(Super::class) other: Any?): Boolean
}

expect class ExplicitInCommonNoEB : Super {
    override fun equals(other: Any?): Boolean
}

expect class ExplicitInPlatform : Super
expect class ExplicitInPlatformNoEB : Super

expect class ExplicitInBoth : Super {
    override fun equals(@EqualityBound(Super::class) other: Any?): Boolean
}

expect open class P

expect class Q : P {
    override fun equals(@EqualityBound(P::class) other: Any?): Boolean
}

expect open class PI {
    override fun equals(@EqualityBound(PI::class) other: Any?): Boolean
}

// MODULE: platform()()(common)
// FILE: platformFile.kt
package p

actual class A {
    actual override fun equals(@EqualityBound(A::class) other: Any?): Boolean = true
}

actual class ExplicitInCommon: Super()
actual class ExplicitInCommonNoEB : Super()

actual class ExplicitInPlatform : Super() {
    override fun equals(@EqualityBound(Super::class) other: Any?): Boolean = true
}

actual class ExplicitInPlatformNoEB : Super() {
    override fun equals(other: Any?): Boolean = true
}

actual class ExplicitInBoth : Super() {
    actual override fun equals(other: Any?): Boolean = true
}

actual open class P

actual class Q : P() {
    actual override fun equals(@EqualityBound(P::class) other: Any?): Boolean = true
}

interface I {
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean
}

actual open class PI : I {
    actual override fun equals(@EqualityBound(PI::class) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, classReference, expect, functionDeclaration, nullableType, operator,
override */
