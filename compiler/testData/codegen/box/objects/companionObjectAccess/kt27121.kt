// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND: WASM

interface A {
    fun test() = ok()

    private companion object {
        fun ok() = "OK"
    }
}

class C : A

fun box() = C().test()
