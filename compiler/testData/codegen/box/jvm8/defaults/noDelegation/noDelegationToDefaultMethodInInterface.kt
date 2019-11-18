// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

interface Test {
    @JvmDefault
    fun test() {
    }
}

interface Test2 : Test {

}

fun box(): String {
    try {
        Test2::class.java.getDeclaredMethod("test")
    }
    catch (e: NoSuchMethodException) {
        return "OK"
    }
    return "fail"
}
