// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

class C {
    fun foo(): Result<String> = Result.success("OK")
}

fun box() = C().foo().getOrThrow()
