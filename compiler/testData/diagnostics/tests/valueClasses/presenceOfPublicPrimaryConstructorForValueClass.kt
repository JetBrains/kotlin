// FIR_IDENTICAL
// SKIP_JAVAC
// ALLOW_KOTLIN_PACKAGE
// FIR_IDENTICAL
// LANGUAGE: +InlineClasses

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class ConstructorWithDefaultVisibility(val x: Int)
@JvmInline
value class PublicConstructor public constructor(val x: Int)
@JvmInline
value class InternalConstructor internal constructor(val x: Int)
@JvmInline
value class ProtectedConstructor protected constructor(val x: Int)
@JvmInline
value class PrivateConstructor private constructor(val x: Int)
