// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND: JVM_IR

class Outer {
    private companion object {
        val result = "OK"
    }

    class Nested {
        fun foo() = object {
            override fun toString(): String = result
        }
    }

    fun test() = Nested().foo().toString()
}

fun box() = Outer().test()