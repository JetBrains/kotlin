// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

interface Test {
    fun test() {
    }
}

interface Test2 : Test {

}

interface Test3 : Test2 {

}

fun box(): String {
    try {
        Test3::class.java.getDeclaredMethod("test")
    }
    catch (e: NoSuchMethodException) {
        return "OK"
    }
    return "fail"
}
