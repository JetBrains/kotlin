// LANGUAGE: +SealedInlineClasses
// SKIP_TXT

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class IC

@JvmInline
<!INLINE_CLASS_UNDERLYING_VALUE_IS_SUBCLASS_OF_ANOTHER_UNDERLYING_VALUE!>sealed<!> value class ICC: IC()

@JvmInline
<!INLINE_CLASS_UNDERLYING_VALUE_IS_SUBCLASS_OF_ANOTHER_UNDERLYING_VALUE!>sealed<!> value class ICC2: IC()
