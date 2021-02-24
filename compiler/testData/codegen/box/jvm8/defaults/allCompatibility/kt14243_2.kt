// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Z<T> {
    fun test(p: T): T {
        return p
    }
}

@JvmDefaultWithoutCompatibility
open class ZImpl : Z<String>

//TODO: this is redundant, revise diagnostic
@JvmDefaultWithoutCompatibility
open class ZImpl2 : Z<String>, ZImpl()

class ZImpl3 : ZImpl2() {

    override fun test(p: String): String {
        return super.test(p)
    }
}

fun box(): String {
    return ZImpl3().test("OK")
}
