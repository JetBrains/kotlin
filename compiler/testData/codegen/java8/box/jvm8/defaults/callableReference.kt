// !API_VERSION: 1.3
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test {
    @kotlin.annotations.JvmDefault
    fun foo(): String = "O"

    @kotlin.annotations.JvmDefault
    val bar: String
        get() = "K"

    fun test(): String {
        return (::foo)() + (::bar)()
    }
}

class TestClass : Test {

}

fun box(): String {
    return TestClass().test()
}
