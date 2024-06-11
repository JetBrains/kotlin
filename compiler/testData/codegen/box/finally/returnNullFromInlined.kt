inline fun foo() = Unit

fun box(): String {
    foo()
    return "OK"
}
