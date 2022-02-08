// WITH_STDLIB

fun foo(): Result<String> = Result.success("OK")

fun box() = foo().getOrThrow()
