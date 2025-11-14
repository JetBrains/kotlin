// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-77008
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: 2.0.0 2.1.0 2.2.0
// ^^^ KT-77008 is fixed in 2.3.0-Beta2

interface I

interface I2 : I {
    fun func(): String
}

class A : I2 {
    override fun func(): String = "OK"
}

class B : I2 {
    override fun func(): String ="Fail B"
}

fun <T : I2> materialize(): T {
    return A() as T
}

var b = true

fun myRun(x: () -> String): String = x()

fun box(): String {
    var i: I = object : I {}

    return myRun {
        i = when (b) {
            true -> materialize()
            else -> B()
        }

        i.func()
    }
}
