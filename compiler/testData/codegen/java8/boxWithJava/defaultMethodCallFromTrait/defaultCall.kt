trait KTrait : Simple {
    fun bar(): String {
        return test("O") + Simple.testStatic("O")
    }
}

class Test : KTrait {}

fun box(): String {
    val test = Test().bar()
    if (test != "OKOK") return "fail $test"

    return "OK"
}