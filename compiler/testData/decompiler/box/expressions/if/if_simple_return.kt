fun foo() = 42

fun box(): String {
    val f = foo()
    return if (f == 40) {
        "FAIL"
    } else {
        "OK"
    }
}
