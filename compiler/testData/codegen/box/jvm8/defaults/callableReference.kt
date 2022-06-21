// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Test {
    @JvmDefault
    fun foo(): String = "O"

    @JvmDefault
    val bar: String
        get() = "K"

    fun test(): String {
        return (::foo).let { it() } + (::bar).let { it() }
    }
}

class TestClass : Test {

}

fun box(): String {
    return TestClass().test()
}
