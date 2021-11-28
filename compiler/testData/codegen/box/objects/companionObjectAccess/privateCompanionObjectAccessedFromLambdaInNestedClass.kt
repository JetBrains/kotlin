// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField

fun <T> eval(fn: () -> T) = fn()

class Outer {
    private companion object {
        val result = "OK"
    }

    class Nested {
        fun foo() = eval { result }
    }

    fun test() = Nested().foo()
}

fun box() = Outer().test()