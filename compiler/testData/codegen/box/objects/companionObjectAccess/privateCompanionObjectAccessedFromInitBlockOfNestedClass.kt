// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField

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