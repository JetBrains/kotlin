// LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField

// FILE: lib.kt
inline fun <T> run(fn: () -> T) = fn()

// FILE: main.kt
class Outer {
    private companion object {
        val result = "OK"
    }

    class Nested {
        fun foo() = run { result }
    }

    fun test() = Nested().foo()
}

fun box() = Outer().test()