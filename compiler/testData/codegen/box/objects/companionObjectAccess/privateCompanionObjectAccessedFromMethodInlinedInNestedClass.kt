// LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// FILE: lib.kt
class Outer {
    private companion object {
        val result = "OK"
    }

    private inline fun bar() = result

    class Nested {
        fun foo(x: Outer) = x.bar()
    }

    fun test() = Nested().foo(this)
}

// FILE: main.kt
fun box() = Outer().test()