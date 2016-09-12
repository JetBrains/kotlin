// FILE: 1.kt
interface Test {
    fun test() {
    }
}

// FILE: 2.kt
// JVM_TARGET: 1.8
interface Test2 : Test {

}

fun box(): String {
    try {
        Test2::class.java.getDeclaredMethod("test")
    }
    catch (e: NoSuchMethodException) {
        return "fail"
    }
    return "OK"
}
