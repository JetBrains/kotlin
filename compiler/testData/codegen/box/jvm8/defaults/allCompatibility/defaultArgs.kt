// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK

interface Test {
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
