// WITH_STDLIB
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: FAKE_OVERRIDE_ISSUES
// On wasm this will produce conflicting return types, Result.<get-value> will return Any but we will try to interpret it as String.
// Before wasm native strings this worked by chance because we added unbox intrinsic for strings.

interface I {
    fun foo(): Result<String>
}

fun box() = object : I {
    override fun foo() = Result.success("OK")
}.foo().getOrThrow()