// !API_VERSION: 1.3
// JVM_TARGET: 1.8
// WITH_RUNTIME
interface Z {
    @JvmDefault
    fun test(s: String = "OK"): String {
        return s
    }
}

class Test: Z

fun box(): String {
    return Test().test()
}