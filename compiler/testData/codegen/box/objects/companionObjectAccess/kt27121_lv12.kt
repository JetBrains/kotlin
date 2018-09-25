// LANGUAGE_VERSION: 1.2
// IGNORE_BACKEND: JS_IR

interface A {
    fun test() = ok()

    private companion object {
        fun ok() = "OK"
    }
}

class C : A

fun box() = C().test()
