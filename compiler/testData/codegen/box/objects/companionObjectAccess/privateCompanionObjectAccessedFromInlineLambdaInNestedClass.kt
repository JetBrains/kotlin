// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND_FIR: JVM_IR

inline fun <T> run(fn: () -> T) = fn()

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