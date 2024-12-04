enum class Test(val x: Int, val str: String) {
    OK;
    constructor(x: Int = 0) : this(x, "OK")
}

fun box(): String =
        Test.OK.str
