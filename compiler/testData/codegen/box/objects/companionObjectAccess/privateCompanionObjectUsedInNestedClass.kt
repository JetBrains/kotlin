// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND: WASM

class Outer {
    private companion object {
        override fun toString(): String = "OK"
    }

    class Nested {
        fun foo(): Any = Outer.Companion
    }

    fun test() = Nested().foo().toString()
}

fun box() = Outer().test()