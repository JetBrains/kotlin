// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// TARGET_BACKEND: JVM

object Test {

    fun test() = { createWildcard("OK") }()

    @JvmStatic
    private fun createWildcard(s: String) = Type<Any>(s).x

    class Type<T>(val x: String)

}

fun box() = Test.test()