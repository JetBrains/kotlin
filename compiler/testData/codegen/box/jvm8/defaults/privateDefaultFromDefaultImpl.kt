// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

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
