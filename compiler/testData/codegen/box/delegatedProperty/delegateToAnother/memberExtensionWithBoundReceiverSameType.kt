// WITH_STDLIB

class A(val prop: String) {
    val A.x: String by ::prop
}

fun box(): String {
    with(A("OK")) {
        return A("fail").x
    }
}
