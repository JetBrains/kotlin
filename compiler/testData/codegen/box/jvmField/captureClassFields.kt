// TARGET_BACKEND: JVM

// WITH_RUNTIME

open class A {
    @JvmField public val publicField = "1";
    @JvmField internal val internalField = "2";
    @JvmField protected val protectedField = "34";

    fun test(): String {
        return {
            publicField + internalField + protectedField
        }()
    }
}


fun box(): String {
    return if (A().test() == "1234") return "OK" else "fail"
}
