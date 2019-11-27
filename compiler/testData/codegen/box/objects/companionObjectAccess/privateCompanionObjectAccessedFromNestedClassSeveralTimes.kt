// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND_FIR: JVM_IR

class Outer {
    private companion object {
        fun xo() = "O"
        fun xk() = "K"
    }

    class Nested1 {
        fun foo() = xo()
    }

    class Nested2 {
        fun bar() = xk()
    }

    fun test() = Nested1().foo() + Nested2().bar()
}

fun box() = Outer().test()