// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: FAKE_OVERRIDE_ISSUES
// On wasm this will produce conflicting return types, foo will return Any but we will try to interpret it as String.
// Before wasm native strings this worked by chance because we added unbox intrinsic for strings.

open class Foo {
    open fun foo(x: CharSequence = "O"): CharSequence = x
}
class Bar<T : String>: Foo() {
    override fun foo(x: CharSequence): T {   // Note the covariant return type
        return (x.toString() + "K") as T
    }
}

fun box() = Bar<String>().foo()
