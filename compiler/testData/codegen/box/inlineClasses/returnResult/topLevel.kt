// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun foo(): Result<String> = Result.success("OK")

fun box() = foo().getOrThrow()
