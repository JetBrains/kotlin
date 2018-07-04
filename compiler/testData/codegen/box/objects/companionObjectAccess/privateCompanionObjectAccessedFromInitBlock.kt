// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField

class Outer {
    private companion object {
        val result = "OK"
    }

    val test: String

    init {
        test = result
    }
}

fun box() = Outer().test