// WITH_STDLIB
// IGNORE_BACKEND_K2: JS_IR, NATIVE
enum class Test(val x: Int, val str: String) {
    OK;
    constructor(x: Int = 0) : this(x, "OK")
}

fun box(): String =
        Test.OK.str