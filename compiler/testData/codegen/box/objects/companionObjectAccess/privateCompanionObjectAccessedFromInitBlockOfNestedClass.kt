// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND_FIR: JVM_IR

class Outer {
    private companion object {
        val result = "OK"
    }

    class Nested {
        val test: String

        init {
            test = result
        }
    }
}

fun box() = Outer.Nested().test