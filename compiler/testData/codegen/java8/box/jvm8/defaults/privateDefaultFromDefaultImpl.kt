// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test {
    fun test(): String {
        return privateFun() + privateProp
    }

    @JvmDefault
    private fun privateFun(): String {
        return "O"
    }

    @JvmDefault
    private val privateProp: String
        get() = "K"
}

class TestImpl: Test

fun box(): String {
    return TestImpl().test()
}