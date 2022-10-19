// FIR_IDENTICAL
// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class Parent

value class WithoutConstructor: Parent()

value class WithEmptyConstructor(): Parent()

value class WithMultipleConstructorParameters(val a: Int, val b: Int): Parent()

