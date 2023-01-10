// LANGUAGE: +SealedInlineClasses
// SKIP_TXT

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class IC

@JvmInline
<!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>sealed<!> value class ICC: IC()

@JvmInline
<!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>sealed<!> value class ICC2: IC()
