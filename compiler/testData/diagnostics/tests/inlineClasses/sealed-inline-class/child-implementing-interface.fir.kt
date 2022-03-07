// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC

package kotlin.jvm

annotation class JvmInline

interface I

@JvmInline
sealed value class SIC

value object O: SIC(), I