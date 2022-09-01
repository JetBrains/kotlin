// LANGUAGE: +SealedInlineClasses
// SKIP_TXT

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed <!SEALED_INLINE_CLASS_WITH_PRIMARY_CONSTRUCTOR!>value<!> class Result(val value: Any)
