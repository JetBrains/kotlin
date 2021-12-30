// WITH_STDLIB

class C {
    fun foo(): Result<String> = Result.success("OK")
}

fun box() = C().foo().getOrThrow()
