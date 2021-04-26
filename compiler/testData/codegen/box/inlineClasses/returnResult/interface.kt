// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

interface I {
    fun foo(): Result<String>
}

fun box() = object : I {
    override fun foo() = Result.success("OK")
}.foo().getOrThrow()