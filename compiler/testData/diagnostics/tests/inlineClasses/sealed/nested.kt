// LANGUAGE: +InlineClasses, -JvmInlineValueClasses, +SealedInlineClasses
// IGNORE_BACKED: JVM

sealed inline class Result {
    inline class Ok(val value: Any): Result()
    inline class FileNotFound(val value: <!INLINE_CLASS_UNDERLYING_VALUE_IS_SUBCLASS_OF_ANOTHER_UNDERLYING_VALUE!>Int<!>): Result()
    class Error(val value: Throwable): Result()
}