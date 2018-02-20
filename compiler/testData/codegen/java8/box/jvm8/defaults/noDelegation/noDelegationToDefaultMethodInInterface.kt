// !API_VERSION: 1.3
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK
interface Test {
    @kotlin.annotations.JvmDefault
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
