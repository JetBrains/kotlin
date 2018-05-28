// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: compatibility
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK
interface Test {
    @JvmDefault
    fun test(s: String ="OK"): String {
        return s
    }
}

class TestClass : Test {

}

fun box(): String {
    val defaultImpls = java.lang.Class.forName(Test::class.java.canonicalName + "\$DefaultImpls")

    val declaredMethod = defaultImpls.getDeclaredMethod("test\$default", Test::class.java, String::class.java, Int::class.java, Any::class.java)
    return declaredMethod.invoke(null, TestClass(), null, 1, null) as String
}
