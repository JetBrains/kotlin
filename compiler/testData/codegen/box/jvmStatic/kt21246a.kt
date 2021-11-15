// WITH_STDLIB
// TARGET_BACKEND: JVM

object Test {

    fun test() = { createWildcard("OK") }()

    @JvmStatic
    private fun createWildcard(s: String) = Type<Any>(s).x

    class Type<T>(val x: String)

}

fun box() = Test.test()