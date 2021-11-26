// WITH_STDLIB
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: FAKE_OVERRIDE_ISSUES
// On wasm this will produce conflicting return types, foo will return Any but we will try to interpret it as String.
// Before wasm native strings this worked by chance because we added unbox intrinsic for strings.

interface I<T> {
    fun foo(): T
}

class C : I<Result<String>> {
    override fun foo(): Result<String> = Result.success("OK")
}

fun box(): String {
    if (((C() as I<Result<String>>).foo() as Result<String>).getOrThrow() != "OK") return "FAIL 1"
    return C().foo().getOrThrow()
}
