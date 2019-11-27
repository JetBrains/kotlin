// !LANGUAGE: -ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND_FIR: JVM_IR

interface A {
    fun test() = ok()

    private companion object {
        fun ok() = "OK"
    }
}

class C : A

fun box() = C().test()
