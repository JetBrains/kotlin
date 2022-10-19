// LANGUAGE: +SealedInlineClasses
// SKIP_TXT

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class Result<!SEALED_INLINE_CLASS_WITH_PRIMARY_CONSTRUCTOR!>(val value: Any)<!>
