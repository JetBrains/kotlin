// FIR_IDENTICAL
// KT-54748

sealed class Result<String> {
  data class Success(val value: String) : Result<String>()
  class Failure(val cause: Throwable) : Result<String>()
}

fun foo(): Result<String> = Result.Success("...")
