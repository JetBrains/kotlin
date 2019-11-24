// WITH_RUNTIME

fun box(): String {
    run {
        test("ok")
        test("ok", 200)
    }
    test("ok")
    test("ok", 300)

    return "OK"
}

private fun test(arg1: String, default: Int = 0) = Unit
