var result = "Fail"

fun foo() {
    result = "OK"
}

fun box(): String {
    val x = ::foo
    x()
    return result
}
