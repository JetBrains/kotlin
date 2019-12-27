// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField

interface A {
    fun test() = ok()

    private companion object {
        fun ok() = "OK"
    }
}

class C : A

fun box() = C().test()
