class Test : Simple {}

fun box(): String {
    val test = Test().test("O")
    if (test != "OK") return "fail $test"

    return Simple.testStatic("O")
}