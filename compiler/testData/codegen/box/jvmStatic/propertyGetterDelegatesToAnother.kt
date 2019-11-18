// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// TARGET_BACKEND: JVM
object ObjectThisTest {

    val testValue: String
        @JvmStatic get() = this.testValue2

    val testValue2: String
        get() = "OK"
}

fun box() = ObjectThisTest.testValue
