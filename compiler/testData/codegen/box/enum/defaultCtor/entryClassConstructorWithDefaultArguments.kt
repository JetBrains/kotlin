// IGNORE_BACKEND_FIR: JVM_IR
enum class Test(val str: String = "OK") {
    OK {
        fun foo() {}
    }
}

fun box(): String =
        Test.OK.str