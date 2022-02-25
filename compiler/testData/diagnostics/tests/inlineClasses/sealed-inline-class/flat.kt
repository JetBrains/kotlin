// LANGUAGE: +SealedInlineClasses

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class Result

@JvmInline
value class Ok(val value: Any): Result()
@JvmInline
value class FileNotFound(val value: <!INLINE_CLASS_UNDERLYING_VALUE_IS_SUBCLASS_OF_ANOTHER_UNDERLYING_VALUE!>Int<!>): Result()
value class Error(val value: Throwable): Result()