fun splus(s: String?, x: Any?) = s + x

fun box(): String {
    val test1 = null + ""
    if (test1 != "null") throw AssertionError("Fail: $test1")

    val ns: String? = "abc"
    val test2 = ns + ""
    if (test2 != "abc") throw AssertionError("Fail: $test2")

    val test3 = splus(null, null)
    if (test3 != "nullnull") throw AssertionError("Fail: $test3")

    return "OK"
}