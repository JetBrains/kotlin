fun test(p: String?): String {
    return "${p ?: "Default"} test"
}
fun box(): String {
    if (test(null) != "Default test") return "fail 1: ${test(null)}"
    if (test("Good") != "Good test") return "fail 1: ${test("OK")}"

    return "OK"
}