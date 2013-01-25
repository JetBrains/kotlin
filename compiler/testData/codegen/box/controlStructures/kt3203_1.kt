fun testIf() {
    val condition = true
    val result = if (condition) {
        val hello: String? = "hello"
        if (hello == null) {
            false
        }
        else {
            true
        }
    }
    else true
    if (!result) throw AssertionError("result is false")
}

fun box(): String {
    testIf()
    return "OK"
}
