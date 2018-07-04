// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND: JVM_IR

class Outer {
    private companion object {
        val result = "OK"
    }

    class Nested {
        fun foo() = { result }()
    }

    fun test() = Nested().foo()
}

fun box() = Outer().test()