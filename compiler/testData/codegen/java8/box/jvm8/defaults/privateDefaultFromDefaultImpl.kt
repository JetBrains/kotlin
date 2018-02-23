// !API_VERSION: 1.3
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test {
    fun test(): String {
        return privateFun() + privateProp
    }

    @kotlin.annotations.JvmDefault
    private fun privateFun(): String {
        return "O"
    }

    @kotlin.annotations.JvmDefault
    private val privateProp: String
        get() = "K"
}

class TestImpl: Test

fun box(): String {
    return TestImpl().test()
}