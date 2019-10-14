fun foo() = 42

fun box(): String {
    val f = foo()
    val b = 50
    if (f > foo()) {
        return "FAIL"
    } else {
        return "OK"
    }
}
