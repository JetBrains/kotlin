// LANGUAGE: +SealedInlineClasses
// SKIP_TXT

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class IC

@JvmInline
sealed <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class ICC: IC()

@JvmInline
sealed <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class ICC2: IC()
