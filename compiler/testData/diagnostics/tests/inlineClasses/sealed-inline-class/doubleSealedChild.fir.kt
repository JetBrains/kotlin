// LANGUAGE: +SealedInlineClasses
// SKIP_TXT

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class IC

@JvmInline
sealed value class ICC: IC()

@JvmInline
sealed value class ICC2: IC()