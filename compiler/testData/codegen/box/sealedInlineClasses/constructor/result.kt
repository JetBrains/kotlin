// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +SealedInlineClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class Result<out T> {
    OPTIONAL_JVM_INLINE_ANNOTATION
    value class Ok<T>(val value: T): Result<T>()
    value class Throwable(val error: kotlin.Throwable): Result<Nothing>()
}

fun box(): String = Result.Ok("OK").value