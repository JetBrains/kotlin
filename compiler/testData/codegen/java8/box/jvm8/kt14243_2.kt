// JVM_TARGET: 1.8
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM8_TARGET_WITH_DEFAULTS

interface Z<T> {
    fun test(p: T): T {
        return p
    }
}

open class ZImpl : Z<String>

open class ZImpl2 : Z<String>, ZImpl()

class ZImpl3 : ZImpl2() {

    override fun test(p: String): String {
        return super.test(p)
    }
}

fun box(): String {
    return ZImpl3().test("OK")
}