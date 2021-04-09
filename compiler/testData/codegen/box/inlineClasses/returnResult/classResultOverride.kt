// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

interface I {
    fun foo(): Result<String>
}

class C : I {
    override fun foo(): Result<String> = Result.success("OK")
}


fun box(): String {
    if ((C() as I).foo().getOrThrow() != "OK") return "FAIL 1"
    return C().foo().getOrThrow()
}
