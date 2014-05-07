fun foo() = "OK"

fun box(): String {
    val x = ::foo
    return x()
}
