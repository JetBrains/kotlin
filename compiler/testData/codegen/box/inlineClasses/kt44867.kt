// WITH_STDLIB
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: FAKE_OVERRIDE_ISSUES
// On wasm this will produce conflicting return types, Result.<get-value> will return Any but we will try to interpret it as String.
// Before wasm native strings this worked by chance because we added unbox intrinsic for strings.

open class BaseWrapper<T>(val response: T)
class Wrapper(result: Result<String>) : BaseWrapper<Result<String>>(result)

fun box(): String {
    return Wrapper(Result.success("OK")).response.getOrThrow()
}
