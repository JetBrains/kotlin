// LANGUAGE: +SealedInlineClasses

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class Result

@JvmInline
value class Ok(val value: Any): Result()
@JvmInline
value class FileNotFound(val value: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>Int<!>): Result()
value class Error(val value: Throwable): Result()
