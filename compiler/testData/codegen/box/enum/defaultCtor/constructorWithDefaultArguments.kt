// IGNORE_BACKEND: JS_IR
enum class Test(val str: String = "OK") {
    OK
}

fun box(): String =
        Test.OK.str