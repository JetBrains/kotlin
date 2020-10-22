// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
interface I

interface Foo<in L : I, in M>

interface Bar<T> {
    fun test(x: Foo<*, T>)
}

fun foo(x: Any) {
    if (x is Bar<*>) {
        x::test
    }
}

fun box(): String = "OK"
