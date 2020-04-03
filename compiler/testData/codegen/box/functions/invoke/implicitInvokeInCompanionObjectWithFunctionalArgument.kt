class TestClass {
    companion object {
        inline operator fun <T> invoke(task: () -> T) = task()
    }
}

fun box(): String {
    val test1 = TestClass { "K" }
    if (test1 != "K") return "fail1, 'test1' == $test1"

    val ok = "OK"

    val x = TestClass { return ok }
}
